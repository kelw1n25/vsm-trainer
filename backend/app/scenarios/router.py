from typing import Any

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.deps import get_current_employee
from app.db import get_db
from app.profiles.models import Employee
from app.scenarios.models import Scenario, parse_definition
from app.scenarios.schema import ScenarioDefinition
from app.scenarios.story_map import StoryMap, build_story_map

router = APIRouter(prefix="/api/scenarios", tags=["scenarios"])


class ScenarioSummary(BaseModel):
    id: str
    title: str
    category: str
    difficulty: int
    service_class: str
    route: str
    description: str
    # Номера ситуаций из справочника, которые отрабатывает сценарий
    situations: list[int]
    endings_total: int


@router.get("", summary="Каталог сценариев", dependencies=[Depends(get_current_employee)])
def list_scenarios(db: Session = Depends(get_db)) -> list[ScenarioSummary]:
    scenarios = db.scalars(select(Scenario).order_by(Scenario.category, Scenario.difficulty, Scenario.title))
    summaries = []
    for scenario in scenarios:
        definition = parse_definition(scenario)
        summaries.append(ScenarioSummary(
            id=scenario.id,
            title=scenario.title,
            category=scenario.category,
            difficulty=scenario.difficulty,
            service_class=scenario.service_class,
            route=scenario.route,
            description=definition.description,
            situations=definition.situations,
            endings_total=sum(node.outcome is not None for node in definition.nodes),
        ))
    return summaries


@router.get("/{scenario_id}/story-map", summary="Архив веток: открытые развилки и финалы сотрудника")
def story_map(
    scenario_id: str,
    employee: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> StoryMap:
    return build_story_map(db, employee, scenario_id)


@router.get("/schema", summary="JSON-схема файла сценария")
def scenario_schema() -> dict[str, Any]:
    return ScenarioDefinition.model_json_schema()
