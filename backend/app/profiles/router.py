import uuid
from datetime import datetime

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.achievements.models import EmployeeAchievement
from app.achievements.service import AchievementOut, LevelOut, describe, level_for
from app.auth.deps import get_current_employee
from app.db import get_db
from app.engine.models import RunStatus, ScenarioRun
from app.game_config import game_config
from app.profiles.avatar import Avatar, avatar_of
from app.profiles.models import Employee, Role
from app.scenarios.models import Scenario

router = APIRouter(prefix="/api/profile", tags=["profile"])

HISTORY_LIMIT = 20


class ProfileAchievement(AchievementOut):
    earned_at: datetime | None


class HistoryItem(BaseModel):
    run_id: uuid.UUID
    scenario_id: str
    scenario_title: str
    category: str
    outcome: RunStatus
    xp_earned: int
    loyalty: int
    safety: int
    finished_at: datetime


class Profile(BaseModel):
    id: int
    full_name: str
    personnel_number: str
    role: Role
    brigade: str
    depot: str
    level: LevelOut
    runs_completed: int
    competence_points: dict[str, int]
    avatar: Avatar
    achievements: list[ProfileAchievement]
    history: list[HistoryItem]


@router.get("", summary="Профиль текущего сотрудника: уровень, компетенции, ачивки, история")
def get_profile(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> Profile:
    earned = {
        a.code: a.earned_at
        for a in db.scalars(select(EmployeeAchievement).where(EmployeeAchievement.employee_id == employee.id))
    }
    runs = db.execute(
        select(ScenarioRun, Scenario)
        .join(Scenario, Scenario.id == ScenarioRun.scenario_id)
        .where(ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS)
        .order_by(ScenarioRun.finished_at.desc())
        .limit(HISTORY_LIMIT)
    )
    return Profile(
        id=employee.id,
        full_name=employee.full_name,
        personnel_number=employee.personnel_number,
        role=employee.role,
        brigade=employee.brigade.name,
        depot=employee.brigade.depot.name,
        level=level_for(employee.xp),
        runs_completed=db.scalar(
            select(func.count()).where(
                ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS
            )
        ),
        competence_points={code: employee.competence_points.get(code, 0) for code in game_config.competences},
        avatar=avatar_of(employee.avatar),
        # Все ачивки из конфига: полученные — с датой, остальные — как цель
        achievements=[
            ProfileAchievement(**describe(code).model_dump(), earned_at=earned.get(code))
            for code in game_config.achievements
        ],
        history=[
            HistoryItem(
                run_id=run.id,
                scenario_id=scenario.id,
                scenario_title=scenario.title,
                category=scenario.category,
                outcome=run.status,
                xp_earned=run.xp_earned,
                loyalty=run.loyalty,
                safety=run.safety,
                finished_at=run.finished_at,
            )
            for run, scenario in runs
        ],
    )


@router.put("/avatar", summary="Сохранить аватар: фон, головной убор и галстук из готовых вариантов")
def update_avatar(
    avatar: Avatar, employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)
) -> Avatar:
    employee.avatar = avatar.model_dump()
    db.commit()
    return avatar
