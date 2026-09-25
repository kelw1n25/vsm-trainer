"""Разбор после сценария: собирается из журнала событий прохождения."""

import uuid

from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.engine.service import final_text
from app.errors import api_error
from app.profiles.models import Employee
from app.scenarios.models import Scenario
from app.scenarios.schema import ScenarioDefinition


class DebriefStep(BaseModel):
    kind: str  # "choice" или "timeout"
    situation: str
    chosen_text: str | None
    explanation: str | None
    was_best: bool
    best_text: str | None
    best_explanation: str | None
    loyalty_delta: int
    safety_delta: int
    loyalty_after: int
    safety_after: int
    competences: dict[str, int]
    elapsed_seconds: float | None
    timer_seconds: int | None


class Debrief(BaseModel):
    run_id: uuid.UUID
    scenario_id: str
    scenario_title: str
    outcome: RunStatus
    reason: str
    final_text: str
    xp_earned: int
    competence_points: dict[str, int]
    initial_loyalty: int
    initial_safety: int
    loyalty: int
    safety: int
    decisions: int
    best_decisions: int
    timeouts: int
    average_reaction_seconds: float | None
    steps: list[DebriefStep]


def build_debrief(db: Session, employee: Employee, run_id: uuid.UUID) -> Debrief:
    run = db.get(ScenarioRun, run_id)
    if run is None or run.employee_id != employee.id:
        raise api_error(404, "run_not_found", "Прохождение не найдено")
    if run.status == RunStatus.IN_PROGRESS:
        raise api_error(409, "run_in_progress", "Разбор доступен после завершения сценария")

    scenario = db.get(Scenario, run.scenario_id)
    definition = ScenarioDefinition.model_validate(scenario.definition)
    nodes = {node.id: node for node in definition.nodes}
    events = db.scalars(
        select(Event).where(Event.run_id == run.id, Event.type.in_(["choice_made", "timeout"])).order_by(Event.id)
    )

    steps = []
    for event in events:
        payload = event.payload
        # Сценарий могли изменить после прохождения — тогда часть текста берём «как есть» из журнала
        node = nodes.get(payload["node_id"])
        choices = {choice.id: choice for choice in node.choices} if node else {}
        best = next((choice for choice in choices.values() if choice.best), None)
        chosen = choices.get(payload.get("choice_id"))
        is_choice = event.type == "choice_made"
        steps.append(
            DebriefStep(
                kind="choice" if is_choice else "timeout",
                situation=node.situation if node else "Шаг удалён из сценария",
                chosen_text=chosen.text if chosen else None,
                explanation=chosen.explanation if chosen else None,
                was_best=is_choice and payload.get("best", False),
                best_text=best.text if best else None,
                best_explanation=best.explanation if best else None,
                loyalty_delta=payload["loyalty_delta"],
                safety_delta=payload["safety_delta"],
                loyalty_after=payload["loyalty_after"],
                safety_after=payload["safety_after"],
                competences=payload["competences"],
                elapsed_seconds=payload.get("elapsed_seconds"),
                timer_seconds=payload.get("timer_seconds"),
            )
        )

    choice_steps = [step for step in steps if step.kind == "choice"]
    reactions = [step.elapsed_seconds for step in choice_steps if step.elapsed_seconds is not None]
    return Debrief(
        run_id=run.id,
        scenario_id=scenario.id,
        scenario_title=scenario.title,
        outcome=run.status,
        reason=run.finish_reason,
        final_text=final_text(run, nodes[run.current_node_id]),
        xp_earned=run.xp_earned,
        competence_points=run.competence_points,
        initial_loyalty=definition.initial.loyalty,
        initial_safety=definition.initial.safety,
        loyalty=run.loyalty,
        safety=run.safety,
        decisions=len(choice_steps),
        best_decisions=sum(step.was_best for step in choice_steps),
        timeouts=len(steps) - len(choice_steps),
        average_reaction_seconds=round(sum(reactions) / len(reactions), 1) if reactions else None,
        steps=steps,
    )
