from typing import Any

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.deps import get_current_employee
from app.db import get_db
from app.scenarios.models import Scenario
from app.scenarios.schema import ScenarioDefinition

router = APIRouter(prefix="/api/scenarios", tags=["scenarios"])


class ScenarioSummary(BaseModel):
    id: str
    title: str
    category: str
    difficulty: int
    service_class: str
    route: str
    demo: bool


@router.get("", summary="Каталог сценариев", dependencies=[Depends(get_current_employee)])
def list_scenarios(db: Session = Depends(get_db)) -> list[ScenarioSummary]:
    scenarios = db.scalars(select(Scenario).order_by(Scenario.category, Scenario.difficulty, Scenario.title))
    return [
        ScenarioSummary(
            id=s.id,
            title=s.title,
            category=s.category,
            difficulty=s.difficulty,
            service_class=s.service_class,
            route=s.route,
            demo=s.definition["demo"],
        )
        for s in scenarios
    ]


@router.get("/schema", summary="JSON-схема файла сценария")
def scenario_schema() -> dict[str, Any]:
    return ScenarioDefinition.model_json_schema()
