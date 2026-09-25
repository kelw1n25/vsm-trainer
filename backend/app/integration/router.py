"""API для внешних систем: HR (сотрудники) и LMS (результаты обучения). Доступ по X-API-Key."""

import hmac
import uuid
from datetime import datetime

from fastapi import APIRouter, Depends, Header, Path, Query
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.achievements.models import EmployeeAchievement
from app.achievements.service import level_for
from app.config import settings
from app.db import get_db
from app.engine.models import RunStatus, ScenarioRun
from app.errors import api_error
from app.game_config import game_config
from app.profiles.models import Brigade, Depot, Employee, Role
from app.scenarios.models import Scenario


def require_api_key(x_api_key: str | None = Header(None, description="Ключ интеграции")) -> None:
    # compare_digest сравнивает за одинаковое время: ключ нельзя подобрать по времени ответа
    if x_api_key is None or not hmac.compare_digest(x_api_key, settings.integration_api_key):
        raise api_error(401, "invalid_api_key", "Неверный или отсутствующий API-ключ (заголовок X-API-Key)")


router = APIRouter(prefix="/api/integration", tags=["integration"], dependencies=[Depends(require_api_key)])

PersonnelNumber = Path(pattern=r"^\d{4,20}$", description="Табельный номер")
RESULTS_LIMIT_MAX = 500


class CompetenceProfile(BaseModel):
    personnel_number: str
    full_name: str
    role: Role
    brigade: str
    depot: str
    level: int
    level_title: str
    xp: int
    competences: dict[str, int]
    achievements: list[str]
    runs_completed: int
    success_rate: float | None
    last_activity_at: datetime | None


class RunResult(BaseModel):
    run_id: uuid.UUID
    personnel_number: str
    scenario_id: str
    scenario_title: str
    category: str
    outcome: RunStatus
    xp_earned: int
    loyalty: int
    safety: int
    finished_at: datetime


class EmployeeUpsert(BaseModel):
    full_name: str = Field(min_length=3, max_length=200)
    role: Role = Role.CONDUCTOR
    brigade: str = Field(min_length=1, max_length=100)
    depot: str = Field(min_length=1, max_length=100)


class EmployeeUpsertResult(BaseModel):
    personnel_number: str
    created: bool


@router.get("/employees/{personnel_number}", summary="Профиль компетенций сотрудника (для HR/LMS)")
def competence_profile(personnel_number: str = PersonnelNumber, db: Session = Depends(get_db)) -> CompetenceProfile:
    employee = db.scalar(select(Employee).where(Employee.personnel_number == personnel_number))
    if employee is None:
        raise api_error(404, "employee_not_found", f"Сотрудник с табельным номером {personnel_number} не найден")
    finished, succeeded = db.execute(
        select(func.count(), func.count().filter(ScenarioRun.status == RunStatus.SUCCESS)).where(
            ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS
        )
    ).one()
    level = level_for(employee.xp)
    return CompetenceProfile(
        personnel_number=employee.personnel_number,
        full_name=employee.full_name,
        role=employee.role,
        brigade=employee.brigade.name,
        depot=employee.brigade.depot.name,
        level=level.level,
        level_title=level.title,
        xp=employee.xp,
        competences={code: employee.competence_points.get(code, 0) for code in game_config.competences},
        achievements=list(db.scalars(
            select(EmployeeAchievement.code).where(EmployeeAchievement.employee_id == employee.id)
        )),
        runs_completed=finished,
        success_rate=round(succeeded / finished, 2) if finished else None,
        last_activity_at=employee.last_activity_at,
    )


@router.get("/results", summary="Завершённые прохождения после момента since (для журнала LMS)")
def results(
    since: datetime = Query(description="Вернуть прохождения, завершённые строго позже (ISO 8601 с часовым поясом)"),
    limit: int = Query(100, ge=1, le=RESULTS_LIMIT_MAX),
    db: Session = Depends(get_db),
) -> list[RunResult]:
    # Для постраничной выгрузки передайте в since finished_at последней полученной записи
    rows = db.execute(
        select(ScenarioRun, Employee.personnel_number, Scenario)
        .join(Employee, Employee.id == ScenarioRun.employee_id)
        .join(Scenario, Scenario.id == ScenarioRun.scenario_id)
        .where(ScenarioRun.finished_at > since)
        .order_by(ScenarioRun.finished_at, ScenarioRun.id)
        .limit(limit)
    )
    return [
        RunResult(
            run_id=run.id,
            personnel_number=number,
            scenario_id=scenario.id,
            scenario_title=scenario.title,
            category=scenario.category,
            outcome=run.status,
            xp_earned=run.xp_earned,
            loyalty=run.loyalty,
            safety=run.safety,
            finished_at=run.finished_at,
        )
        for run, number, scenario in rows
    ]


@router.put("/employees/{personnel_number}", summary="Создать или обновить сотрудника из HR-системы")
def upsert_employee(
    body: EmployeeUpsert, personnel_number: str = PersonnelNumber, db: Session = Depends(get_db)
) -> EmployeeUpsertResult:
    depot = db.scalar(select(Depot).where(Depot.name == body.depot))
    if depot is None:
        depot = Depot(name=body.depot)
        db.add(depot)
        db.flush()
    brigade = db.scalar(select(Brigade).where(Brigade.name == body.brigade, Brigade.depot_id == depot.id))
    if brigade is None:
        brigade = Brigade(name=body.brigade, depot=depot)
        db.add(brigade)

    employee = db.scalar(select(Employee).where(Employee.personnel_number == personnel_number))
    created = employee is None
    if created:
        # Пароль HR не передаёт: вход по паролю закрыт, пока не подключён SSO (см. docs/limitations.md)
        employee = Employee(personnel_number=personnel_number, password_hash="", competence_points={})
        db.add(employee)
    employee.full_name = body.full_name
    employee.role = body.role
    employee.brigade = brigade
    db.commit()
    return EmployeeUpsertResult(personnel_number=personnel_number, created=created)
