"""Шкалы, очки компетенций и XP. Все коэффициенты — в config/game.yaml (раздел scoring)."""

from datetime import datetime

from sqlalchemy.orm import Session

from app.game_config import game_config
from app.profiles.models import Employee

SCALE_MIN, SCALE_MAX = 0, 100


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
