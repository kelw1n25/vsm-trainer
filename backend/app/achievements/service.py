"""Уровни и достижения. Правила — в config/game.yaml (разделы levels и achievements)."""

from datetime import datetime

from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.achievements.models import EmployeeAchievement
from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.game_config import FastStreakRule, ScenarioResultRule, game_config
from app.scenarios.models import Scenario


class LevelOut(BaseModel):
    level: int
    title: str
    xp: int
    level_xp: int
    next_level_xp: int | None


class AchievementOut(BaseModel):
    code: str
    title: str
    description: str


def level_for(xp: int) -> LevelOut:
    levels = game_config.levels
    index = max(i for i, level in enumerate(levels) if xp >= level.xp)
    current = levels[index]
    next_xp = levels[index + 1].xp if index + 1 < len(levels) else None
    return LevelOut(level=current.level, title=current.title, xp=xp, level_xp=current.xp, next_level_xp=next_xp)


def describe(code: str) -> AchievementOut:
    achievement = game_config.achievements[code]
    return AchievementOut(code=code, title=achievement.title, description=achievement.description)


def evaluate(
    db: Session, run: ScenarioRun, scenario: Scenario, initial_scales: tuple[int, int], at: datetime
) -> list[AchievementOut]:
    """Проверяет ещё не полученные ачивки после решения или финала и выдаёт выполненные."""
    earned = set(db.scalars(select(EmployeeAchievement.code).where(EmployeeAchievement.employee_id == run.employee_id)))
    new = []
    for code, achievement in game_config.achievements.items():
        if code in earned:
            continue
        rule = achievement.rule
        if isinstance(rule, FastStreakRule):
            done = _fast_streak(db, run.employee_id, rule)
        else:
            done = run.status != RunStatus.IN_PROGRESS and _scenario_result(db, run, scenario, rule, initial_scales)
        if done:
            db.execute(
                insert(EmployeeAchievement)
                .values(employee_id=run.employee_id, code=code, run_id=run.id, earned_at=at)
                .on_conflict_do_nothing()
            )
            db.add(Event(employee_id=run.employee_id, run_id=run.id, type="achievement_earned", payload={"code": code}))
            new.append(describe(code))
    return new


def _fast_streak(db: Session, employee_id: int, rule: FastStreakRule) -> bool:
    recent = db.scalars(
        select(Event)
        .where(Event.employee_id == employee_id, Event.type.in_(["choice_made", "timeout"]))
        .order_by(Event.id.desc())
        .limit(50)
    )
    # Решения без таймера серию не прерывают и не продолжают; таймаут — прерывает
    timed = [e for e in recent if e.type == "timeout" or e.payload.get("timer_seconds")][: rule.count]
    return len(timed) == rule.count and all(
        e.type == "choice_made" and e.payload["elapsed_seconds"] <= e.payload["timer_seconds"] * rule.timer_fraction
        for e in timed
    )


def _scenario_result(
    db: Session, run: ScenarioRun, scenario: Scenario, rule: ScenarioResultRule, initial_scales: tuple[int, int]
) -> bool:
    if rule.category is not None and scenario.category != rule.category:
        return False
    if run.status not in rule.outcomes:
        return False
    if rule.loyalty_above is not None and run.loyalty <= rule.loyalty_above:
        return False
    if rule.safety_above is not None and run.safety <= rule.safety_above:
        return False
    steps = list(
        db.scalars(select(Event).where(Event.run_id == run.id, Event.type.in_(["choice_made", "timeout"])))
    )
    if rule.no_timeouts and any(e.type == "timeout" for e in steps):
        return False
    if rule.scales_never_below is not None:
        lowest = min([*initial_scales, *(min(e.payload["loyalty_after"], e.payload["safety_after"]) for e in steps)])
        if lowest < rule.scales_never_below:
            return False
    return True
