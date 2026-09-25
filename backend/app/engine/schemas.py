import uuid
from datetime import datetime
from typing import Literal

from pydantic import BaseModel

from app.achievements.service import AchievementOut, LevelOut
from app.engine.models import RunStatus


class StartRunRequest(BaseModel):
    scenario_id: str


class ChoiceRequest(BaseModel):
    # node_id — на какой шаг отвечает клиент: защищает от повторной отправки и ответа на устаревший шаг
    node_id: str
    choice_id: str


class LineOut(BaseModel):
    speaker: str
    name: str | None
    text: str


class ChoiceOut(BaseModel):
    id: str
    text: str


class NodeOut(BaseModel):
    id: str
    situation: str
    line: LineOut | None
    choices: list[ChoiceOut]
    timer_seconds: int | None
    # deadline_at — до какого момента показывать обратный отсчёт;
    # timeout_at — когда сервер применит истечение таймера (дедлайн + льгота на задержку сети)
    deadline_at: datetime | None
    timeout_at: datetime | None


class StepOut(BaseModel):
    """Последствия только что произошедшего шага — показываются сразу после выбора."""

    kind: Literal["choice", "timeout"]
    text: str
    loyalty_delta: int
    safety_delta: int
    competences: dict[str, int]


class FinalOut(BaseModel):
    outcome: RunStatus
    reason: Literal["final", "loyalty_depleted", "safety_depleted"]
    text: str
    xp_earned: int
    competence_points: dict[str, int]


class RunState(BaseModel):
    id: uuid.UUID
    scenario_id: str
    scenario_title: str
    status: RunStatus
    loyalty: int
    safety: int
    node: NodeOut | None
    final: FinalOut | None
    last_steps: list[StepOut]
    # Награды, полученные именно этим запросом — чтобы показать их сразу
    new_achievements: list[AchievementOut]
    level_up: LevelOut | None
    server_time: datetime
