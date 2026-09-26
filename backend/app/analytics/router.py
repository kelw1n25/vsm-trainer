import uuid
from datetime import datetime
from typing import Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.achievements.service import level_for
from app.analytics.models import Event
from app.analytics.service import Analytics, build_analytics
from app.auth.deps import get_current_employee, require_instructor
from app.db import get_db
from app.engine.models import ScenarioRun
from app.errors import api_error
from app.game_config import game_config
from app.notifications.models import Notification
from app.profiles.models import Brigade, Employee, Role

router = APIRouter(prefix="/api/analytics", tags=["analytics"])


class TeamMember(BaseModel):
    employee_id: int
    full_name: str
    brigade: str
    level_title: str
    xp: int
    last_activity_at: datetime | None
    weakest_competence: str | None


class ClientEvent(BaseModel):
    """Действие, которое видит только клиент: сервер не знает, открыт ли экран или закрыто ли приложение."""

    type: Literal["debrief_opened", "notification_opened", "run_exited"]
    run_id: uuid.UUID | None = None
    notification_id: int | None = None
    platform: Literal["web", "ios", "android"]


@router.post("/events", status_code=202, summary="Записать действие пользователя на клиенте")
def record_event(
    body: ClientEvent, employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)
) -> dict[str, str]:
    if body.type in ("debrief_opened", "run_exited"):
        run = db.get(ScenarioRun, body.run_id) if body.run_id else None
        if run is None or run.employee_id != employee.id:
            raise api_error(404, "run_not_found", "Прохождение не найдено")
    payload: dict = {"platform": body.platform}
    if body.type == "notification_opened":
        notification = db.get(Notification, body.notification_id) if body.notification_id else None
        if notification is None or notification.employee_id != employee.id:
            raise api_error(404, "notification_not_found", "Уведомление не найдено")
        payload["notification_type"] = notification.type
    db.add(Event(employee_id=employee.id, run_id=body.run_id, type=body.type, payload=payload))
    db.commit()
    return {"status": "accepted"}


@router.get("/me", summary="Аналитика текущего сотрудника")
def my_analytics(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> Analytics:
    return build_analytics(db, employee)


@router.get("/team", summary="Проводники депо инструктора (только для инструктора)")
def team(instructor: Employee = Depends(require_instructor), db: Session = Depends(get_db)) -> list[TeamMember]:
    members = db.scalars(
        select(Employee)
        .join(Brigade, Brigade.id == Employee.brigade_id)
        .where(Brigade.depot_id == instructor.brigade.depot_id, Employee.role == Role.CONDUCTOR)
        .order_by(Employee.full_name)
    )
    result = []
    for member in members:
        points = {code: member.competence_points.get(code, 0) for code in game_config.competences}
        # Пока сотрудник ничего не проходил, «слабой» компетенции у него нет
        weakest = min(points, key=points.get) if member.last_activity_at else None
        result.append(TeamMember(
            employee_id=member.id,
            full_name=member.full_name,
            brigade=member.brigade.name,
            level_title=level_for(member.xp).title,
            xp=member.xp,
            last_activity_at=member.last_activity_at,
            weakest_competence=game_config.competences[weakest] if weakest else None,
        ))
    return result


@router.get("/employees/{employee_id}", summary="Аналитика проводника своего депо (только для инструктора)")
def employee_analytics(
    employee_id: int, instructor: Employee = Depends(require_instructor), db: Session = Depends(get_db)
) -> Analytics:
    employee = db.get(Employee, employee_id)
    if employee is None or employee.brigade.depot_id != instructor.brigade.depot_id:
        raise api_error(404, "employee_not_found", "Сотрудник не найден в вашем депо")
    return build_analytics(db, employee)
