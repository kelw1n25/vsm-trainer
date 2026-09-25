"""Шкалы, очки компетенций, XP и сгорание баллов. Коэффициенты — в config/game.yaml."""

import math
from datetime import datetime, timedelta

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.game_config import game_config
from app.notifications.service import notify
from app.profiles.models import Employee

SCALE_MIN, SCALE_MAX = 0, 100
TIMEZONE = "Europe/Moscow"


def clamp_scale(value: int) -> int:
    return max(SCALE_MIN, min(SCALE_MAX, value))


def fast_answer_bonus(timer_seconds: int | None, elapsed_seconds: float) -> dict[str, int]:
    """Очки стрессоустойчивости за решение, принятое в первой части таймера."""
    rule = game_config.scoring.fast_answer
    if timer_seconds is None or elapsed_seconds > timer_seconds * rule.timer_fraction:
        return {}
    return {rule.competence: rule.points}


def calculate_xp(outcome: str, difficulty: int, loyalty: int, safety: int) -> int:
    rules = game_config.scoring
    base = rules.xp_by_outcome[outcome] * rules.difficulty_multiplier[difficulty]
    scale_bonus = loyalty * rules.scale_weights.loyalty + safety * rules.scale_weights.safety
    return round(base + scale_bonus)


def award(db: Session, employee_id: int, xp: int, competence_points: dict[str, int], at: datetime) -> Employee:
    # Блокировка строки: два одновременно завершённых сценария не потеряют очки друг друга
    employee = db.get(Employee, employee_id, with_for_update=True)
    employee.xp += xp
    totals = dict(employee.competence_points)
    for code, value in competence_points.items():
        # За сценарий очки компетенции могут уйти в минус, но итог в профиле — не ниже нуля
        totals[code] = max(0, totals.get(code, 0) + value)
    employee.competence_points = totals
    employee.last_activity_at = at
    return employee


def week_start(db: Session) -> datetime:
    return db.scalar(select(func.date_trunc("week", func.now(), TIMEZONE)))


def weekly_challenge_bonus(db: Session, employee_id: int, run_id, scenario_id: str, outcome: str) -> int:
    """Бонус XP за челлендж недели — один раз за календарную неделю, только за успех."""
    challenge = game_config.weekly_challenge
    if challenge is None or scenario_id != challenge.scenario_id or outcome != "success":
        return 0
    already = db.scalar(
        select(Event.id)
        .where(Event.employee_id == employee_id, Event.type == "challenge_completed", Event.created_at >= week_start(db))
        .limit(1)
    )
    if already is not None:
        return 0
    db.add(Event(
        employee_id=employee_id, run_id=run_id, type="challenge_completed",
        payload={"scenario_id": scenario_id, "bonus_xp": challenge.bonus_xp},
    ))
    return challenge.bonus_xp


def burn_anchor(employee: Employee) -> datetime | None:
    """Момент, от которого считаются дни без прохождений: последняя активность или последнее сгорание."""
    if employee.last_activity_at is None:
        return None
    return max(employee.last_activity_at, employee.last_burn_at or employee.last_activity_at)


def burn_due_points(db: Session, now: datetime) -> None:
    """Сжигает XP всем, у кого подошёл срок. Вызывается при запросах — фоновый планировщик не нужен."""
    rules = game_config.points_burn
    period = timedelta(days=rules.inactive_days)
    anchor = func.greatest(Employee.last_activity_at, func.coalesce(Employee.last_burn_at, Employee.last_activity_at))
    # SKIP LOCKED: если другая копия backend уже обрабатывает сотрудника, не ждём и не сжигаем дважды
    due = db.scalars(
        select(Employee)
        .where(Employee.last_activity_at.is_not(None), anchor <= now - period)
        .with_for_update(skip_locked=True)
    )
    for employee in due:
        start = burn_anchor(employee)
        periods = int((now - start) / period)
        xp = employee.xp
        for _ in range(periods):
            xp -= math.floor(xp * rules.burn_fraction)
        burned = employee.xp - xp
        employee.xp = xp
        employee.last_burn_at = start + periods * period
        db.add(Event(employee_id=employee.id, type="points_burned", payload={"burned": burned, "periods": periods}))
        if burned:
            notify(
                db, employee.id, "points_burned", "Баллы сгорели",
                f"Сценарии не проходились {periods * rules.inactive_days} дн.: сгорело {burned} XP. "
                "Пройдите сценарий, чтобы остановить сгорание.",
                f"burned:{employee.last_burn_at.isoformat()}",
            )
