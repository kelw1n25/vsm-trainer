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


class RevealRequest(BaseModel):
    node_id: str


class LineOut(BaseModel):
    speaker: str
    # Имя и должность персонажа; у рассказчика и проводника — null (имя проводника подставляет интерфейс)
    name: str | None
    role: str | None
    kind: Literal["speech", "thought", "narration"]
    text: str
    expression: str | None
    sound: str | None


class SceneCharacterOut(BaseModel):
    id: str
    position: str
    expression: str
    pose: str
    hand: str | None
    item: str | None


class SceneOut(BaseModel):
    background: str
    characters: list[SceneCharacterOut]


class CharacterOut(BaseModel):
    id: str
    name: str
    role: str
    look: dict[str, str | bool]


class ChoiceOut(BaseModel):
    id: str
    text: str


class NodeOut(BaseModel):
    id: str
    scene: SceneOut
    dialogue: list[LineOut]
    # Варианты приходят только после POST /reveal: пока проводник читает сцену, таймер не идёт
    choices_shown: bool
    choices: list[ChoiceOut]
    timer_seconds: int | None
    # deadline_at — до какого момента показывать обратный отсчёт;
    # timeout_at — когда сервер применит истечение таймера (дедлайн + льгота на задержку сети)
    deadline_at: datetime | None
    timeout_at: datetime | None


class StepOut(BaseModel):
    """Последствия только что произошедшего шага — реакция персонажей звучит перед следующей сценой."""

    kind: Literal["choice", "timeout"]
    text: str
    reaction: list[LineOut]
    loyalty_delta: int
    safety_delta: int
    competences: dict[str, int]


class FinalOut(BaseModel):
    outcome: RunStatus
    reason: Literal["final", "loyalty_depleted", "safety_depleted"]
    # Название финала, например «Ситуация разрешена спокойно»
    ending: str
    text: str
    scene: SceneOut
    dialogue: list[LineOut]
    xp_earned: int
    competence_points: dict[str, int]


class RunState(BaseModel):
    id: uuid.UUID
    scenario_id: str
    scenario_title: str
    category: str
    difficulty: int
    route: str
    service_class: str
    characters: list[CharacterOut]
    # Сколько шагов уже пройдено и сколько как минимум осталось до финала — для индикатора истории.
    # Карта будущих развилок не раскрывается
    steps_taken: int
    steps_left: int
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
