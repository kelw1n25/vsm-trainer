from typing import Any

from fastapi import APIRouter

from app.scenarios.schema import ScenarioDefinition

router = APIRouter(prefix="/api/scenarios", tags=["scenarios"])


@router.get("/schema", summary="JSON-схема файла сценария")
def scenario_schema() -> dict[str, Any]:
    return ScenarioDefinition.model_json_schema()
