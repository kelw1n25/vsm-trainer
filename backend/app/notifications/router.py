from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import func, select, update
from sqlalchemy.orm import Session

from app.auth.deps import get_current_employee
from app.db import get_db
from app.engine.models import ScenarioRun
from app.errors import api_error
from app.game_config import game_config
from app.notifications.models import Notification
from app.notifications.service import notify
from app.profiles.models import Employee
from app.scenarios.models import Scenario
from app.scoring import service as scoring

router = APIRouter(prefix="/api/notifications", tags=["notifications"])

LIST_LIMIT = 30
NEW_SCENARIO_DAYS = 14
MSK = timezone(timedelta(hours=3))


class NotificationOut(BaseModel):
    id: int
    type: str
    title: str
    body: str
    created_at: datetime
    read: bool


class NotificationList(BaseModel):
    unread: int
    items: list[NotificationOut]


@router.get("", summary="Уведомления текущего сотрудника (заодно применяет сгорание и выдаёт новые)")
def list_notifications(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> NotificationList:
    _sync(db, employee)
    db.commit()
    items = db.scalars(
        select(Notification)
        .where(Notification.employee_id == employee.id)
        .order_by(Notification.created_at.desc(), Notification.id.desc())
        .limit(LIST_LIMIT)
    ).all()
    unread = db.scalar(
        select(func.count()).where(Notification.employee_id == employee.id, Notification.read_at.is_(None))
    )
    return NotificationList(
        unread=unread,
        items=[
            NotificationOut(id=n.id, type=n.type, title=n.title, body=n.body, created_at=n.created_at, read=n.read_at is not None)
            for n in items
        ],
    )


@router.post("/{notification_id}/read", summary="Отметить уведомление прочитанным")
def mark_read(
    notification_id: int, employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)
) -> dict[str, str]:
    notification = db.get(Notification, notification_id)
    if notification is None or notification.employee_id != employee.id:
        raise api_error(404, "notification_not_found", "Уведомление не найдено")
    notification.read_at = notification.read_at or func.now()
    db.commit()
    return {"status": "ok"}


@router.post("/read-all", summary="Отметить все уведомления прочитанными")
def mark_all_read(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> dict[str, str]:
    db.execute(
        update(Notification)
        .where(Notification.employee_id == employee.id, Notification.read_at.is_(None))
        .values(read_at=func.now())
    )
    db.commit()
    return {"status": "ok"}


def _sync(db: Session, employee: Employee) -> None:
    """Уведомления, которые зависят от времени, создаются при запросе — без фонового планировщика."""
    now = db.scalar(select(func.now()))
    scoring.burn_due_points(db, now)
    db.refresh(employee)

    rules = game_config.points_burn
    anchor = scoring.burn_anchor(employee)
    if anchor is not None:
        warn_from = anchor + timedelta(days=rules.inactive_days - rules.warn_days_before)
        if now >= warn_from:
            burn_at = anchor + timedelta(days=rules.inactive_days)
            notify(
                db, employee.id, "burn_warning", "Скоро сгорят баллы",
                f"Пройдите любой сценарий до {burn_at.astimezone(MSK):%d.%m %H:%M}, иначе сгорит "
                f"{round(rules.burn_fraction * 100)}% XP.",
                f"burn_warning:{anchor.isoformat()}",
            )

    challenge = game_config.weekly_challenge
    if challenge and (scenario := db.get(Scenario, challenge.scenario_id)):
        week = scoring.week_start(db)
        notify(
            db, employee.id, "weekly_challenge", "Челлендж недели",
            f"Пройдите «{scenario.title}» на успех до конца недели — бонус +{challenge.bonus_xp} XP.",
            f"challenge:{week.date().isoformat()}",
        )

    started = select(ScenarioRun.scenario_id).where(ScenarioRun.employee_id == employee.id)
    fresh = db.scalars(
        select(Scenario).where(
            Scenario.loaded_at >= now - timedelta(days=NEW_SCENARIO_DAYS), Scenario.id.not_in(started)
        )
    )
    for scenario in fresh:
        notify(
            db, employee.id, "new_scenario", "Новый сценарий",
            f"Доступен сценарий «{scenario.title}».",
            f"new_scenario:{scenario.id}",
        )
