"""Движок прохождения сценария.

Всё состояние прохождения лежит в scenario_runs, поэтому любой запрос может
обработать любая копия backend. Строка прохождения блокируется на время запроса
(SELECT ... FOR UPDATE), чтобы два одновременных ответа не применились дважды.

Таймер серверный: при каждом обращении к прохождению сначала проверяется, не истекло
ли время текущего узла (по часам БД). Клиентский таймер только показывает остаток.
"""

import uuid
from dataclasses import dataclass, field
from datetime import datetime, timedelta

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.achievements import service as achievements
from app.achievements.service import AchievementOut, LevelOut
from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.engine.schemas import ChoiceOut, FinalOut, LineOut, NodeOut, RunState, StepOut
from app.errors import api_error
from app.game_config import game_config
from app.notifications.service import notify
from app.profiles.models import Employee
from app.scenarios.conditions import is_satisfied, parse_condition
from app.scenarios.models import Scenario, parse_definition
from app.scenarios.schema import Choice, Effects, Node, ScenarioDefinition
from app.scoring import service as scoring

DEPLETED_TEXT = {
    "loyalty_depleted": "Лояльность пассажиров упала до нуля: доверие потеряно, ситуация вышла из-под контроля. "
    "Сценарий завершён досрочно.",
    "safety_depleted": "Рейтинг безопасности упал до нуля: пассажиры и экипаж подвергнуты недопустимому риску. "
    "Сценарий завершён досрочно.",
}


@dataclass
class Rewards:
    """Ачивки и повышение уровня, полученные в текущем запросе."""

    achievements: list[AchievementOut] = field(default_factory=list)
    level_up: LevelOut | None = None


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
    if run is not None:
        return get_run(db, employee, run.id)

    definition = parse_definition(scenario)
    now = db_now(db)
    run = ScenarioRun(
        employee_id=employee.id,
        scenario_id=scenario_id,
        current_node_id=definition.start_node,
        loyalty=definition.initial.loyalty,
        safety=definition.initial.safety,
        flags=[],
        competence_points={},
        node_entered_at=now,
    )
    db.add(run)
    db.flush()
    _log(db, run, "run_started", {"scenario_id": scenario_id})
    db.commit()
    return _build_state(db, run, scenario, definition, now, [], Rewards())


def get_run(db: Session, employee: Employee, run_id: uuid.UUID) -> RunState:
    run, scenario, definition = _lock_run(db, employee, run_id)
    now = db_now(db)
    rewards = Rewards()
    steps = _resolve_timeouts(db, run, scenario, definition, now, rewards)
    db.commit()
    return _build_state(db, run, scenario, definition, now, steps, rewards)


def choose(db: Session, employee: Employee, run_id: uuid.UUID, node_id: str, choice_id: str) -> RunState:
    run, scenario, definition = _lock_run(db, employee, run_id)
    now = db_now(db)
    rewards = Rewards()
    if _resolve_timeouts(db, run, scenario, definition, now, rewards):
        # Истечение таймера уже применено и сохраняется, ответ — нет
        db.commit()
        raise api_error(409, "time_expired", "Время на решение истекло, ответ не принят")
    if run.status != RunStatus.IN_PROGRESS:
        raise api_error(409, "run_finished", "Сценарий уже завершён")
    if node_id != run.current_node_id:
        raise api_error(409, "stale_node", "Ответ на этот шаг уже принят. Обновите экран.")

    node = next(n for n in definition.nodes if n.id == run.current_node_id)
    choice = next((c for c in visible_choices(node, run) if c.id == choice_id), None)
    if choice is None:
        raise api_error(422, "unknown_choice", "Такого варианта нет на этом шаге")

    elapsed = (now - run.node_entered_at).total_seconds()
    step = apply_choice(db, run, scenario, definition, choice, elapsed, now, rewards)
    db.commit()
    return _build_state(db, run, scenario, definition, now, [step], rewards)


def _lock_run(db: Session, employee: Employee, run_id: uuid.UUID) -> tuple[ScenarioRun, Scenario, ScenarioDefinition]:
    run = db.scalar(select(ScenarioRun).where(ScenarioRun.id == run_id).with_for_update())
    # Чужое прохождение отвечает так же, как несуществующее: не раскрываем, что оно есть
    if run is None or run.employee_id != employee.id:
        raise api_error(404, "run_not_found", "Прохождение не найдено")
    scenario = db.get(Scenario, run.scenario_id)
    return run, scenario, parse_definition(scenario)


def _resolve_timeouts(
    db: Session,
    run: ScenarioRun,
    scenario: Scenario,
    definition: ScenarioDefinition,
    now: datetime,
    rewards: Rewards,
) -> list[StepOut]:
    """Применяет все истёкшие таймеры по цепочке, если проводник пропустил несколько узлов подряд."""
    nodes = {node.id: node for node in definition.nodes}
    grace = timedelta(seconds=game_config.timer.answer_grace_seconds)
    steps = []
    while run.status == RunStatus.IN_PROGRESS:
        node = nodes[run.current_node_id]
        deadline = _deadline(run, node)
        if deadline is None or now <= deadline + grace:
            break
        # Следующий узел начинается в момент дедлайна, а не в момент запроса
        steps.append(apply_timeout(db, run, scenario, definition, deadline, rewards))
    return steps


def apply_choice(
    db: Session,
    run: ScenarioRun,
    scenario: Scenario,
    definition: ScenarioDefinition,
    choice: Choice,
    elapsed: float,
    at: datetime,
    rewards: Rewards,
) -> StepOut:
    """Один шаг «выбор варианта». Используется API и генератором демо-истории."""
    nodes = {node.id: node for node in definition.nodes}
    node = nodes[run.current_node_id]
    bonus = scoring.fast_answer_bonus(node.timer_seconds, elapsed)
    step = _apply(run, choice.effects, bonus, choice.text, "choice")
    _log(db, run, "choice_made", {
        "node_id": node.id,
        "choice_id": choice.id,
        "best": choice.best,
        "elapsed_seconds": elapsed,
        "timer_seconds": node.timer_seconds,
        **_step_payload(step, run),
    })
    _advance(db, run, scenario, definition, nodes[choice.next], at, rewards)
    return step


def apply_timeout(
    db: Session, run: ScenarioRun, scenario: Scenario, definition: ScenarioDefinition, at: datetime, rewards: Rewards
) -> StepOut:
    """Один шаг «истёк таймер». Используется API и генератором демо-истории."""
    nodes = {node.id: node for node in definition.nodes}
    node = nodes[run.current_node_id]
    step = _apply(run, node.timeout_effects, {}, "Время на решение истекло", "timeout")
    _log(db, run, "timeout", {"node_id": node.id, **_step_payload(step, run)})
    _advance(db, run, scenario, definition, nodes[node.timeout_next], at, rewards)
    return step


def visible_choices(node: Node, run: ScenarioRun) -> list[Choice]:
    scales = {"loyalty": run.loyalty, "safety": run.safety}
    return [
        choice
        for choice in node.choices
        if all(is_satisfied(parse_condition(text), scales, run.flags) for text in choice.condition)
    ]


def _apply(run: ScenarioRun, effects: Effects, bonus: dict[str, int], text: str, kind: str) -> StepOut:
    loyalty_before, safety_before = run.loyalty, run.safety
    run.loyalty = scoring.clamp_scale(run.loyalty + effects.loyalty)
    run.safety = scoring.clamp_scale(run.safety + effects.safety)
    # Новые объекты вместо изменения на месте: так SQLAlchemy видит, что JSONB-поля изменились
    run.flags = sorted(set(run.flags) | set(effects.set_flags))
    gained = dict(effects.competences)
    for code, value in bonus.items():
        gained[code] = gained.get(code, 0) + value
    points = dict(run.competence_points)
    for code, value in gained.items():
        points[code] = points.get(code, 0) + value
    run.competence_points = points
    return StepOut(
        kind=kind,
        text=text,
        # Фактическое изменение с учётом границ 0–100
        loyalty_delta=run.loyalty - loyalty_before,
        safety_delta=run.safety - safety_before,
        competences=gained,
    )


def _advance(
    db: Session,
    run: ScenarioRun,
    scenario: Scenario,
    definition: ScenarioDefinition,
    target: Node,
    at: datetime,
    rewards: Rewards,
) -> None:
    """Переход после шага: досрочный провал при нулевой шкале, иначе вход в следующий узел.

    В конце проверяются ачивки: серия быстрых решений — после каждого шага, итоговые — после финала.
    """
    if run.loyalty == 0 or run.safety == 0:
        reason = "loyalty_depleted" if run.loyalty == 0 else "safety_depleted"
        _finish(db, run, scenario, RunStatus.FAILURE, reason, at, rewards)
    else:
        run.current_node_id = target.id
        run.node_entered_at = at
        if target.outcome is not None:
            _finish(db, run, scenario, RunStatus(target.outcome), "final", at, rewards)
    initial = (definition.initial.loyalty, definition.initial.safety)
    rewards.achievements += achievements.evaluate(db, run, scenario, initial, at)


def _finish(
    db: Session, run: ScenarioRun, scenario: Scenario, outcome: RunStatus, reason: str, at: datetime, rewards: Rewards
) -> None:
    run.status = outcome
    run.finish_reason = reason
    run.finished_at = at
    run.xp_earned = scoring.calculate_xp(
        outcome, scenario.difficulty, run.loyalty, run.safety
    ) + scoring.weekly_challenge_bonus(db, run.employee_id, run.id, scenario.id, outcome)
    employee = scoring.award(db, run.employee_id, run.xp_earned, run.competence_points, at)
    before, after = achievements.level_for(employee.xp - run.xp_earned), achievements.level_for(employee.xp)
    if after.level > before.level:
        rewards.level_up = after
        _log(db, run, "level_up", {"level": after.level, "title": after.title})
        notify(
            db, run.employee_id, "level_up", f"Новый уровень: {after.title}",
            f"Вы достигли уровня {after.level}. Так держать!", f"level:{after.level}",
        )
    _log(db, run, "run_finished", {
        "outcome": outcome,
        "reason": reason,
        "category": scenario.category,
        "xp_earned": run.xp_earned,
        "competence_points": run.competence_points,
        "loyalty": run.loyalty,
        "safety": run.safety,
    })


def final_text(run: ScenarioRun, final_node: Node) -> str:
    return final_node.final_text if run.finish_reason == "final" else DEPLETED_TEXT[run.finish_reason]


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
    db: Session,
    run: ScenarioRun,
    scenario: Scenario,
    definition: ScenarioDefinition,
    now: datetime,
    steps: list[StepOut],
    rewards: Rewards,
) -> RunState:
    node = next(n for n in definition.nodes if n.id == run.current_node_id)
    node_out = final_out = None
    if run.status == RunStatus.IN_PROGRESS:
        deadline = _deadline(run, node)
        grace = timedelta(seconds=game_config.timer.answer_grace_seconds)
        node_out = NodeOut(
            id=node.id,
            situation=node.situation,
            line=LineOut(**node.line.model_dump()) if node.line else None,
            choices=[ChoiceOut(id=c.id, text=c.text) for c in visible_choices(node, run)],
            timer_seconds=node.timer_seconds,
            deadline_at=deadline,
            timeout_at=deadline + grace if deadline else None,
        )
    else:
        final_out = FinalOut(
            outcome=run.status,
            reason=run.finish_reason,
            text=final_text(run, node),
            xp_earned=run.xp_earned,
            competence_points=run.competence_points,
        )
    return RunState(
        id=run.id,
        scenario_id=scenario.id,
        scenario_title=scenario.title,
        category=scenario.category,
        difficulty=scenario.difficulty,
        route=scenario.route,
        service_class=scenario.service_class,
        steps_taken=db.scalar(
            select(func.count()).where(Event.run_id == run.id, Event.type.in_(["choice_made", "timeout"]))
        ),
        status=run.status,
        loyalty=run.loyalty,
        safety=run.safety,
        node=node_out,
        final=final_out,
        last_steps=steps,
        new_achievements=rewards.achievements,
        level_up=rewards.level_up,
        server_time=now,
    )
