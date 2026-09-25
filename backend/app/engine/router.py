import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.auth.deps import get_current_employee
from app.db import get_db
from app.engine import service
from app.engine.schemas import ChoiceRequest, RunState, StartRunRequest
from app.profiles.models import Employee

router = APIRouter(prefix="/api/runs", tags=["runs"])


@router.post("", summary="Начать сценарий (или продолжить незавершённый)")
def start_run(
    body: StartRunRequest,
    employee: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> RunState:
    return service.start_run(db, employee, body.scenario_id)


@router.get("/{run_id}", summary="Текущее состояние прохождения")
def get_run(
    run_id: uuid.UUID,
    employee: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> RunState:
    return service.get_run(db, employee, run_id)


@router.post("/{run_id}/choices", summary="Выбрать вариант на текущем шаге")
def choose(
    run_id: uuid.UUID,
    body: ChoiceRequest,
    employee: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> RunState:
    return service.choose(db, employee, run_id, body.node_id, body.choice_id)
