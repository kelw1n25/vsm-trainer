from collections import Counter
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.game_config import game_config
from app.scenarios.conditions import parse_condition

Id = Annotated[str, Field(pattern=r"^[a-z0-9_-]+$", max_length=100)]
Flag = Annotated[str, Field(pattern=r"^\w+$", max_length=50)]
Delta = Annotated[int, Field(ge=-100, le=100)]
ScaleValue = Annotated[int, Field(ge=0, le=100)]


class StrictModel(BaseModel):
    # Опечатка в имени поля (например, «timout_next») — ошибка, а не молча проигнорированное поле
    model_config = ConfigDict(extra="forbid")


class Effects(StrictModel):
    loyalty: Delta = 0
    safety: Delta = 0
    set_flags: list[Flag] = []
    competences: dict[str, Delta] = {}

    @field_validator("competences")
    @classmethod
    def known_competences(cls, value: dict[str, int]) -> dict[str, int]:
        unknown = sorted(set(value) - set(game_config.competences))
        if unknown:
            raise ValueError(
                f"неизвестные компетенции: {', '.join(unknown)}. "
                f"Допустимые: {', '.join(game_config.competences)}"
            )
        return value


class Line(StrictModel):
    speaker: Literal["passenger", "colleague", "train_chief"]
    name: str | None = None
    text: str


class Choice(StrictModel):
    id: Id
    text: str
    condition: list[str] = []
    effects: Effects = Field(default_factory=Effects)
    next: Id
    explanation: str = Field(min_length=1)
    best: bool = False

    @field_validator("condition")
    @classmethod
    def parseable_conditions(cls, value: list[str]) -> list[str]:
        for text in value:
            parse_condition(text)
        return value


class Node(StrictModel):
    id: Id
    situation: str | None = None
    line: Line | None = None
    timer_seconds: Annotated[int, Field(gt=0, le=300)] | None = None
    timeout_next: Id | None = None
    timeout_effects: Effects = Field(default_factory=Effects)
    choices: list[Choice] = []
    outcome: Literal["success", "partial", "failure"] | None = None
    final_text: str | None = None

    @model_validator(mode="after")
    def check_node_kind(self) -> "Node":
        if self.outcome is not None:
            if self.choices or self.timer_seconds is not None:
                raise ValueError("у финального узла (outcome) не может быть вариантов выбора и таймера")
            if not self.final_text:
                raise ValueError("у финального узла должен быть итоговый текст final_text")
            return self

        if not self.situation:
            raise ValueError("нужно описание ситуации situation")
        if not self.choices:
            raise ValueError("нужны варианты выбора (choices), а для финального узла — outcome")
        if (self.timer_seconds is None) != (self.timeout_next is None):
            raise ValueError("timer_seconds и timeout_next задаются только вместе")
        if self.timer_seconds is not None:
            effects = self.timeout_effects
            if effects.loyalty > 0 or effects.safety > 0 or (effects.loyalty == 0 and effects.safety == 0):
                raise ValueError(
                    "истечение таймера должно снижать хотя бы одну шкалу и не повышать другую "
                    "(timeout_effects.loyalty / timeout_effects.safety)"
                )

        duplicates = [cid for cid, count in Counter(c.id for c in self.choices).items() if count > 1]
        if duplicates:
            raise ValueError(f"повторяются id вариантов: {', '.join(duplicates)}")
        if sum(choice.best for choice in self.choices) != 1:
            raise ValueError("ровно один вариант должен быть помечен best: true — он показывается в разборе")
        if self.timer_seconds is None and all(choice.condition for choice in self.choices):
            raise ValueError(
                "у всех вариантов есть условия и нет таймера — проводник может оказаться в тупике. "
                "Добавьте вариант без условия или таймер"
            )
        return self


class InitialScales(StrictModel):
    loyalty: ScaleValue
    safety: ScaleValue


class ScenarioDefinition(StrictModel):
    id: Id
    title: str
    category: Literal["conflict", "medical", "service", "safety"]
    difficulty: Annotated[int, Field(ge=1, le=3)]
    service_class: str
    route: str
    # Короткое описание для страницы сценария (без спойлеров развилок)
    description: str = ""
    # true — контент написан командой, а не взят из материалов кейсодержателя
    demo: bool = False
    initial: InitialScales
    start_node: Id
    nodes: list[Node] = Field(min_length=1)
