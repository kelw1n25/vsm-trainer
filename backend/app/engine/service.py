"""Движок прохождения сценария.

Всё состояние прохождения лежит в scenario_runs, поэтому любой запрос может
обработать любая копия backend. Строка прохождения блокируется на время запроса
(SELECT ... FOR UPDATE), чтобы два одновременных ответа не применились дважды.
"""

import uuid
from datetime import datetime, timedelta

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.engine.schemas import ChoiceOut, FinalOut, LineOut, NodeOut, RunState, StepOut
from app.errors import api_error
from app.profiles.models import Employee
from app.scenarios.conditions import is_satisfied, parse_condition
from app.scenarios.models import Scenario
from app.scenarios.schema import Choice, Effects, Node, ScenarioDefinition


def db_now(db: Session) -> datetime:
    """Время берём из БД, а не из часов конкретной копии backend: у всех копий оно одинаковое."""
    return db.scalar(select(func.now()))


def start_run(db: Session, employee: Employee, scenario_id: str) -> RunState:
    scenario = db.get(Scenario, scenario_id)
    if scenario is None:
        raise api_error(404, "scenario_not_found", f"Сценарий «{scenario_id}» не найден")

    # Повторный старт возвращает незавершённое прохождение, а не плодит новые
    run = db.scalar(
        select(ScenarioRun).where(
            ScenarioRun.employee_id == employee.id,
            ScenarioRun.scenario_id == scenario_id,
            ScenarioRun.status == RunStatus.IN_PROGRESS,
        )
    )
    definition = ScenarioDefinition.model_validate(scenario.definition)
    if run is None:
        run = ScenarioRun(
            employee_id=employee.id,
            scenario_id=scenario_id,
            current_node_id=definition.start_node,
            loyalty=definition.initial.loyalty,
            safety=definition.initial.safety,
            flags=[],
            competence_points={},
            node_entered_at=db_now(db),
        )
        db.add(run)
        db.flush()
        _log(db, run, "run_started", {"scenario_id": scenario_id})
        db.commit()
    return _build_state(run, scenario, definition, db_now(db), [])


def get_run(db: Session, employee: Employee, run_id: uuid.UUID) -> RunState:
    run, scenario, definition = _lock_run(db, employee, run_id)
    db.commit()
    return _build_state(run, scenario, definition, db_now(db), [])


def choose(db: Session, employee: Employee, run_id: uuid.UUID, node_id: str, choice_id: str) -> RunState:
    run, scenario, definition = _lock_run(db, employee, run_id)
    now = db_now(db)
    if run.status != RunStatus.IN_PROGRESS:
        raise api_error(409, "run_finished", "Сценарий уже завершён")
    if node_id != run.current_node_id:
        raise api_error(409, "stale_node", "Ответ на этот шаг уже принят. Обновите экран.")

    nodes = {node.id: node for node in definition.nodes}
    node = nodes[run.current_node_id]
    choice = next((c for c in _visible_choices(node, run) if c.id == choice_id), None)
    if choice is None:
        raise api_error(422, "unknown_choice", "Такого варианта нет на этом шаге")

    step = _apply(run, choice.effects, choice.text, "choice")
    _log(db, run, "choice_made", {
        "node_id": node.id,
        "choice_id": choice.id,
        "elapsed_seconds": (now - run.node_entered_at).total_seconds(),
        **_step_payload(step, run),
    })
    _enter_node(db, run, nodes[choice.next], now)
    db.commit()
    return _build_state(run, scenario, definition, now, [step])


def _lock_run(db: Session, employee: Employee, run_id: uuid.UUID) -> tuple[ScenarioRun, Scenario, ScenarioDefinition]:
    run = db.scalar(select(ScenarioRun).where(ScenarioRun.id == run_id).with_for_update())
    # Чужое прохождение отвечает так же, как несуществующее: не раскрываем, что оно есть
    if run is None or run.employee_id != employee.id:
        raise api_error(404, "run_not_found", "Прохождение не найдено")
    scenario = db.get(Scenario, run.scenario_id)
    return run, scenario, ScenarioDefinition.model_validate(scenario.definition)


def _visible_choices(node: Node, run: ScenarioRun) -> list[Choice]:
    scales = {"loyalty": run.loyalty, "safety": run.safety}
    return [
        choice
        for choice in node.choices
        if all(is_satisfied(parse_condition(text), scales, run.flags) for text in choice.condition)
    ]


def _apply(run: ScenarioRun, effects: Effects, text: str, kind: str) -> StepOut:
    run.loyalty += effects.loyalty
    run.safety += effects.safety
    # Новые объекты вместо изменения на месте: так SQLAlchemy видит, что JSONB-поля изменились
    run.flags = sorted(set(run.flags) | set(effects.set_flags))
    points = dict(run.competence_points)
    for code, value in effects.competences.items():
        points[code] = points.get(code, 0) + value
    run.competence_points = points
    return StepOut(
        kind=kind,
        text=text,
        loyalty_delta=effects.loyalty,
        safety_delta=effects.safety,
        competences=effects.competences,
    )


def _enter_node(db: Session, run: ScenarioRun, node: Node, entered_at: datetime) -> None:
    run.current_node_id = node.id
    run.node_entered_at = entered_at
    if node.outcome is not None:
        run.status = RunStatus(node.outcome)
        run.finished_at = entered_at
        _log(db, run, "run_finished", {"outcome": node.outcome, "node_id": node.id})


def _deadline(run: ScenarioRun, node: Node) -> datetime | None:
    if node.timer_seconds is None:
        return None
    return run.node_entered_at + timedelta(seconds=node.timer_seconds)


def _step_payload(step: StepOut, run: ScenarioRun) -> dict:
    return {
        "loyalty_delta": step.loyalty_delta,
        "safety_delta": step.safety_delta,
        "competences": step.competences,
        "loyalty_after": run.loyalty,
        "safety_after": run.safety,
    }


def _log(db: Session, run: ScenarioRun, event_type: str, payload: dict) -> None:
    db.add(Event(employee_id=run.employee_id, run_id=run.id, type=event_type, payload=payload))


def _build_state(
    run: ScenarioRun, scenario: Scenario, definition: ScenarioDefinition, now: datetime, steps: list[StepOut]
) -> RunState:
    node = next(n for n in definition.nodes if n.id == run.current_node_id)
    node_out = final_out = None
    if run.status == RunStatus.IN_PROGRESS:
        node_out = NodeOut(
            id=node.id,
            situation=node.situation,
            line=LineOut(**node.line.model_dump()) if node.line else None,
            choices=[ChoiceOut(id=c.id, text=c.text) for c in _visible_choices(node, run)],
            timer_seconds=node.timer_seconds,
            deadline_at=_deadline(run, node),
        )
    else:
        final_out = FinalOut(outcome=run.status, text=node.final_text)
    return RunState(
        id=run.id,
        scenario_id=scenario.id,
        scenario_title=scenario.title,
        status=run.status,
        loyalty=run.loyalty,
        safety=run.safety,
        node=node_out,
        final=final_out,
        last_steps=steps,
        server_time=now,
    )
