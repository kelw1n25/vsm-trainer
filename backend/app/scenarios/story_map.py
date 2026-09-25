"""Архив веток сценария: какие развилки и финалы сотрудник уже открыл.

Карта строится по журналу его решений на сервере. Тексты неисследованных вариантов
и названия неоткрытых финалов не отдаются вовсе — их нельзя подсмотреть даже в ответе API.
"""

from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.engine.models import RunStatus, ScenarioRun
from app.errors import api_error
from app.profiles.models import Employee
from app.scenarios.models import Scenario, parse_definition


class MapChoice(BaseModel):
    id: str
    explored: bool
    # Текст и переход известны только для исследованных вариантов
    text: str | None
    next: str | None


class MapNode(BaseModel):
    id: str
    situation: str
    choices: list[MapChoice]
    timer: bool
    timeout_explored: bool
    timeout_next: str | None


class MapEnding(BaseModel):
    id: str
    outcome: str
    reached: bool
    ending: str | None


class StoryMap(BaseModel):
    scenario_id: str
    start_node: str
    playthroughs: int
    choices_total: int
    choices_explored: int
    nodes: list[MapNode]
    endings: list[MapEnding]


def build_story_map(db: Session, employee: Employee, scenario_id: str) -> StoryMap:
    scenario = db.get(Scenario, scenario_id)
    if scenario is None:
        raise api_error(404, "scenario_not_found", f"Сценарий «{scenario_id}» не найден")
    definition = parse_definition(scenario)

    runs = db.scalars(
        select(ScenarioRun).where(ScenarioRun.employee_id == employee.id, ScenarioRun.scenario_id == scenario_id)
    ).all()
    events = db.scalars(
        select(Event).where(
            Event.run_id.in_([run.id for run in runs]), Event.type.in_(["choice_made", "timeout"])
        )
    ).all()
    chosen = {(e.payload["node_id"], e.payload["choice_id"]) for e in events if e.type == "choice_made"}
    timed_out = {e.payload["node_id"] for e in events if e.type == "timeout"}
    reached = {run.current_node_id for run in runs if run.status != RunStatus.IN_PROGRESS and run.finish_reason == "final"}

    # Узел виден на карте, если сотрудник в нём побывал: старт, цель исследованного перехода или текущий узел
    visited = {definition.start_node} | {run.current_node_id for run in runs}
    nodes = {node.id: node for node in definition.nodes}
    for node in definition.nodes:
        visited |= {choice.next for choice in node.choices if (node.id, choice.id) in chosen}
        if node.id in timed_out and node.timeout_next:
            visited.add(node.timeout_next)

    map_nodes = []
    for node in definition.nodes:
        if node.outcome is not None or node.id not in visited:
            continue
        choices = []
        for choice in node.choices:
            explored = (node.id, choice.id) in chosen
            choices.append(MapChoice(
                id=choice.id,
                explored=explored,
                text=choice.text if explored else None,
                next=choice.next if explored else None,
            ))
        timeout_explored = node.id in timed_out
        map_nodes.append(MapNode(
            id=node.id,
            situation=node.situation,
            choices=choices,
            timer=node.timer_seconds is not None,
            timeout_explored=timeout_explored,
            timeout_next=node.timeout_next if timeout_explored else None,
        ))

    endings = [
        MapEnding(id=node.id, outcome=node.outcome, reached=node.id in reached, ending=node.ending if node.id in reached else None)
        for node in nodes.values()
        if node.outcome is not None
    ]
    decision_nodes = [node for node in definition.nodes if node.outcome is None]
    return StoryMap(
        scenario_id=scenario_id,
        start_node=definition.start_node,
        playthroughs=sum(run.status != RunStatus.IN_PROGRESS for run in runs),
        choices_total=sum(len(node.choices) for node in decision_nodes),
        choices_explored=len(chosen),
        nodes=map_nodes,
        endings=endings,
    )
