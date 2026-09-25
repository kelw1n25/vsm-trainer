from typing import Literal

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.achievements.service import level_for
from app.auth.deps import get_current_employee
from app.db import get_db
from app.engine.models import ScenarioRun
from app.profiles.models import Brigade, Depot, Employee, Role

router = APIRouter(prefix="/api/leaderboard", tags=["leaderboard"])

TOP_SIZE = 20
# Неделя и месяц начинаются по московскому времени, а не по часовому поясу сервера БД
TIMEZONE = "Europe/Moscow"

Scope = Literal["brigade", "depot", "company"]
Period = Literal["week", "month", "all"]


class LeaderboardRow(BaseModel):
    rank: int
    employee_id: int
    full_name: str
    brigade: str
    depot: str
    level_title: str
    points: int
    is_me: bool


class Leaderboard(BaseModel):
    scope: Scope
    period: Period
    title: str
    participants: int
    rows: list[LeaderboardRow]


@router.get("", summary="Рейтинг: бригада / депо / компания за неделю / месяц / всё время")
def leaderboard(
    scope: Scope = Query("brigade"),
    period: Period = Query("week"),
    me: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> Leaderboard:
    if period == "all":
        # За всё время — XP профиля: он уже учитывает сгорание баллов
        points = Employee.xp
        source = select(Employee)
    else:
        # Неделя и месяц — календарные: рейтинг обнуляется в начале периода
        period_xp = (
            select(ScenarioRun.employee_id, func.sum(ScenarioRun.xp_earned).label("xp"))
            .where(ScenarioRun.finished_at >= func.date_trunc(period, func.now(), TIMEZONE))
            .group_by(ScenarioRun.employee_id)
            .subquery()
        )
        points = func.coalesce(period_xp.c.xp, 0)
        source = select(Employee).outerjoin(period_xp, period_xp.c.employee_id == Employee.id)

    ranked = (
        source.add_columns(
            Brigade.name.label("brigade"),
            Depot.name.label("depot"),
            points.label("points"),
            func.rank().over(order_by=points.desc()).label("rank"),
        )
        .join(Brigade, Brigade.id == Employee.brigade_id)
        .join(Depot, Depot.id == Brigade.depot_id)
        .where(Employee.role == Role.CONDUCTOR)
    )
    if scope == "brigade":
        ranked = ranked.where(Employee.brigade_id == me.brigade_id)
        title = me.brigade.name
    elif scope == "depot":
        ranked = ranked.where(Brigade.depot_id == me.brigade.depot_id)
        title = me.brigade.depot.name
    else:
        title = "Вся компания"

    ranked = ranked.subquery()
    total = db.scalar(select(func.count()).select_from(ranked))
    # Топ плюс строка текущего пользователя, даже если он ниже топа
    rows = db.execute(
        select(ranked)
        .where(or_(ranked.c.rank <= TOP_SIZE, ranked.c.id == me.id))
        .order_by(ranked.c.rank, ranked.c.full_name)
    ).all()
    return Leaderboard(
        scope=scope,
        period=period,
        title=title,
        participants=total,
        rows=[
            LeaderboardRow(
                rank=row.rank,
                employee_id=row.id,
                full_name=row.full_name,
                brigade=row.brigade,
                depot=row.depot,
                level_title=level_for(row.xp).title,
                points=row.points,
                is_me=row.id == me.id,
            )
            for row in rows
        ],
    )
