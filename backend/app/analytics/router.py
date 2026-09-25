from datetime import datetime

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.achievements.service import level_for
from app.analytics.service import Analytics, build_analytics
from app.auth.deps import get_current_employee, require_instructor
from app.db import get_db
from app.errors import api_error
from app.game_config import game_config
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
