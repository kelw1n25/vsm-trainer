"""Аналитика по сотруднику: прогресс, компетенции, типичные ошибки, рекомендация.

Всё считается из журнала событий и прохождений. Выводы формулируются правилами
с порогами — чтобы их можно было объяснить, а не «так решила модель».
"""

from collections import defaultdict
from datetime import datetime

from pydantic import BaseModel
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.game_config import game_config
from app.profiles.models import Employee
from app.scenarios.models import Scenario, parse_definition
from app.scoring.service import TIMEZONE

WEEKS = 8
# Вывод о категории делаем, только если в ней набралось достаточно шагов
MIN_STEPS = 3
TIMEOUT_RATE_ALERT = 0.3
BEST_RATE_ALERT = 0.5
SLOW_REACTION_ALERT = 0.6

CATEGORY_LABELS = {"conflict": "Конфликт", "medical": "Медицина", "service": "Сервис", "safety": "Безопасность"}
CATEGORY_PHRASES = {
    "conflict": "в конфликтных сценариях",
    "medical": "в медицинских сценариях",
    "service": "в сервисных сценариях",
    "safety": "в сценариях безопасности",
}


class WeekPoint(BaseModel):
    week_start: datetime
    xp: int
    runs: int
    successes: int


class CategoryStats(BaseModel):
    category: str
    title: str
    runs: int
    decisions: int
    timed_steps: int
    success_rate: float | None
    best_choice_rate: float | None
    timeout_rate: float | None
    average_reaction_share: float | None  # доля таймера, потраченная на решение


class CompetenceStat(BaseModel):
    code: str
    title: str
    points: int


class Recommendation(BaseModel):
    scenario_id: str
    title: str
    reason: str


class Analytics(BaseModel):
    employee_id: int
    full_name: str
    total_runs: int
    progress: list[WeekPoint]
    categories: list[CategoryStats]
    competences: list[CompetenceStat]
    strengths: list[str]
    weaknesses: list[str]
    mistakes: list[str]
    recommendation: Recommendation | None


def build_analytics(db: Session, employee: Employee) -> Analytics:
    runs = db.execute(
        select(ScenarioRun, Scenario.category)
        .join(Scenario, Scenario.id == ScenarioRun.scenario_id)
        .where(ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS)
    ).all()
    steps = db.execute(
        select(Event, Scenario.category)
        .join(ScenarioRun, ScenarioRun.id == Event.run_id)
        .join(Scenario, Scenario.id == ScenarioRun.scenario_id)
        .where(Event.employee_id == employee.id, Event.type.in_(["choice_made", "timeout"]))
    ).all()

    categories = _category_stats(runs, steps)
    competences = [
        CompetenceStat(code=code, title=title, points=employee.competence_points.get(code, 0))
        for code, title in game_config.competences.items()
    ]
    ranked = sorted(competences, key=lambda c: c.points)
    played = bool(runs)
    return Analytics(
        employee_id=employee.id,
        full_name=employee.full_name,
        total_runs=len(runs),
        progress=_progress(db, employee.id),
        categories=categories,
        competences=competences,
        strengths=[c.title for c in reversed(ranked[-2:]) if c.points > 0] if played else [],
        weaknesses=[c.title for c in ranked[:2]] if played else [],
        mistakes=_mistakes(categories, runs),
        recommendation=_recommend(db, employee, ranked[0].code if played else None),
    )


def _progress(db: Session, employee_id: int) -> list[WeekPoint]:
    week = func.date_trunc("week", ScenarioRun.finished_at, TIMEZONE).label("week")
    since = func.date_trunc("week", func.now(), TIMEZONE) - func.make_interval(0, 0, WEEKS - 1)
    rows = db.execute(
        select(
            week,
            func.sum(ScenarioRun.xp_earned),
            func.count(),
            func.count().filter(ScenarioRun.status == RunStatus.SUCCESS),
        )
        .where(ScenarioRun.employee_id == employee_id, ScenarioRun.finished_at >= since)
        .group_by(week)
        .order_by(week)
    )
    return [WeekPoint(week_start=w, xp=xp, runs=count, successes=ok) for w, xp, count, ok in rows]


def _rate(part: int, whole: int) -> float | None:
    return round(part / whole, 2) if whole else None


def _category_stats(runs, steps) -> list[CategoryStats]:
    by_category: dict[str, dict] = defaultdict(lambda: defaultdict(list))
    for run, category in runs:
        by_category[category]["runs"].append(run)
    for event, category in steps:
        by_category[category]["steps"].append(event)

    result = []
    for category, data in sorted(by_category.items()):
        category_runs = data["runs"]
        choices = [e for e in data["steps"] if e.type == "choice_made"]
        timed = [e for e in data["steps"] if e.type == "timeout" or e.payload.get("timer_seconds")]
        reactions = [
            e.payload["elapsed_seconds"] / e.payload["timer_seconds"] for e in choices if e.payload.get("timer_seconds")
        ]
        result.append(CategoryStats(
            category=category,
            title=CATEGORY_LABELS.get(category, category),
            runs=len(category_runs),
            decisions=len(choices),
            timed_steps=len(timed),
            success_rate=_rate(sum(r.status == RunStatus.SUCCESS for r in category_runs), len(category_runs)),
            best_choice_rate=_rate(sum(e.payload.get("best", False) for e in choices), len(choices)),
            timeout_rate=_rate(sum(e.type == "timeout" for e in timed), len(timed)),
            average_reaction_share=round(sum(reactions) / len(reactions), 2) if reactions else None,
        ))
    return result


def _mistakes(categories: list[CategoryStats], runs) -> list[str]:
    mistakes = []
    for stats in categories:
        where = CATEGORY_PHRASES.get(stats.category, f"в категории «{stats.title}»")
        if stats.timed_steps >= MIN_STEPS and stats.timeout_rate >= TIMEOUT_RATE_ALERT:
            mistakes.append(f"Часто истекает таймер {where}: {round(stats.timeout_rate * 100)}% шагов с таймером")
        if stats.decisions >= MIN_STEPS and stats.best_choice_rate < BEST_RATE_ALERT:
            mistakes.append(f"Лучшее решение {where} выбирается редко: {round(stats.best_choice_rate * 100)}% решений")
        if stats.decisions >= MIN_STEPS and (stats.average_reaction_share or 0) >= SLOW_REACTION_ALERT:
            mistakes.append(
                f"Решения {where} принимаются медленно: в среднем {round(stats.average_reaction_share * 100)}% таймера"
            )
    depleted = defaultdict(int)
    for run, _ in runs:
        if run.finish_reason in ("loyalty_depleted", "safety_depleted"):
            depleted[run.finish_reason] += 1
    if depleted["safety_depleted"]:
        mistakes.append(f"Рейтинг безопасности падал до нуля: {depleted['safety_depleted']} раз(а)")
    if depleted["loyalty_depleted"]:
        mistakes.append(f"Лояльность пассажиров падала до нуля: {depleted['loyalty_depleted']} раз(а)")
    return mistakes


def _recommend(db: Session, employee: Employee, weakest: str | None) -> Recommendation | None:
    """Сценарий, где можно набрать больше всего очков в самой слабой компетенции.

    Считается прямо по графу сценария, поэтому новый файл сценария учитывается автоматически.
    """
    succeeded = set(db.scalars(
        select(ScenarioRun.scenario_id).where(
            ScenarioRun.employee_id == employee.id, ScenarioRun.status == RunStatus.SUCCESS
        )
    ))
    scenarios = db.scalars(select(Scenario).order_by(Scenario.difficulty, Scenario.id)).all()
    candidates = [s for s in scenarios if s.id not in succeeded] or scenarios
    if not candidates:
        return None
    if weakest is None:
        first = candidates[0]
        return Recommendation(scenario_id=first.id, title=first.title, reason="Начните с этого сценария — он самый простой.")

    def potential(scenario: Scenario) -> int:
        definition = parse_definition(scenario)
        return sum(
            max(0, choice.effects.competences.get(weakest, 0)) for node in definition.nodes for choice in node.choices
        )

    best = max(candidates, key=potential)
    title = game_config.competences[weakest]
    return Recommendation(
        scenario_id=best.id,
        title=best.title,
        reason=f"Компетенция «{title}» у вас проседает, а в этом сценарии её тренируют больше всего.",
    )
