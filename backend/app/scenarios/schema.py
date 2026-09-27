"""Формат файла сценария — интерактивной визуальной новеллы.

Сценарий — граф узлов. Узел — это сцена (фон и персонажи) с диалогом и моментом решения
или финалом. Проводник читает реплики, а затем выбирает, как поступить; выбор меняет шкалы,
скрытые параметры и флаги и ведёт в следующий узел.
"""

from collections import Counter
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.game_config import game_config
from app.handbook.data import handbook
from app.scenarios.conditions import parse_condition

Id = Annotated[str, Field(pattern=r"^[a-z0-9_-]+$", max_length=100)]
Name = Annotated[str, Field(pattern=r"^[a-z_][a-z0-9_]*$", max_length=50)]
Delta = Annotated[int, Field(ge=-100, le=100)]
ScaleValue = Annotated[int, Field(ge=0, le=100)]
Color = Annotated[str, Field(pattern=r"^#[0-9A-Fa-f]{6}$")]

# Особые говорящие: рассказчик и сам проводник (его имя подставляет интерфейс)
NARRATOR = "narrator"
PLAYER = "player"
Expression = Literal[
    "neutral", "happy", "angry", "annoyed", "worried", "sad", "surprised", "thinking", "serious", "pained", "tipsy"
]
# Звуки реплик: sigh и sigh_long — выдох без голоса (подходит любому персонажу), sigh_male — мужской вздох
Sound = Literal["sigh", "sigh_long", "sigh_male"]
Background = Literal[
    "salon", "salon_evening", "salon_night", "business", "platform", "vestibule", "bistro", "staff_room"
]


class StrictModel(BaseModel):
    # Опечатка в имени поля (например, «timout_next») — ошибка, а не молча проигнорированное поле.
    # Блоки YAML «>» оставляют перевод строки в конце: его срезаем, иначе на телефоне кнопка ответа
    # и абзац получают лишнюю пустую строку (браузер её схлопывает, нативный текст — нет)
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class Look(StrictModel):
    """Внешность персонажа. Сотрудники поезда — в единой форме (outfit: uniform)."""

    outfit: Literal["uniform", "casual"] = "casual"
    top: Color = "#56657F"
    bottom: Color = "#34405A"
    hair: Color = "#2B2320"
    hair_style: Literal["short", "bun", "long"] = "short"
    child: bool = False


class Character(StrictModel):
    name: str
    role: str
    look: Look = Field(default_factory=Look)


class SceneCharacter(StrictModel):
    id: Name
    position: Literal["left", "center", "right"]
    expression: Expression = "neutral"
    # Поза как на картинке сценария: стоит, сидит в кресле поезда или в кресле-коляске
    pose: Literal["stand", "sit", "wheelchair"] = "stand"
    # Жест правой руки и предмет в ней; без жеста рука следует эмоции (например, на груди при боли)
    hand: Literal["down", "point", "hold", "radio", "hush", "chest", "throat"] | None = None
    item: Literal["ticket", "bottle", "cup", "extinguisher", "radio"] | None = None


class Scene(StrictModel):
    background: Background
    characters: list[SceneCharacter] = Field(default_factory=list, max_length=3)

    @field_validator("characters")
    @classmethod
    def distinct_positions(cls, value: list[SceneCharacter]) -> list[SceneCharacter]:
        positions = [character.position for character in value]
        if len(positions) != len(set(positions)):
            raise ValueError("два персонажа в сцене стоят на одной позиции")
        return value


class Line(StrictModel):
    """Реплика персонажа, мысль проводника (thought) или слова рассказчика (speaker: narrator)."""

    speaker: Name
    text: str = Field(min_length=1)
    thought: bool = False
    # Персонаж меняет выражение лица на этой реплике и сохраняет его до следующей смены
    expression: Expression | None = None
    # Звук вместе с репликой — например, вздох уставшего или облегчённо выдохнувшего персонажа
    sound: Sound | None = None

    @model_validator(mode="after")
    def thoughts_are_players(self) -> "Line":
        if self.thought and self.speaker != PLAYER:
            raise ValueError("мысли (thought: true) бывают только у проводника — speaker: player")
        return self


class Effects(StrictModel):
    loyalty: Delta = 0
    safety: Delta = 0
    # Скрытые параметры сценария (доверие, напряжение…): игрок их не видит, но от них зависят ветки
    stats: dict[Name, Delta] = {}
    set_flags: list[Name] = []
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


class Choice(StrictModel):
    id: Id
    text: str
    condition: list[str] = []
    effects: Effects = Field(default_factory=Effects)
    # Как персонажи отреагировали на решение — звучит сразу после выбора, до следующей сцены
    reaction: list[Line] = []
    next: Id
    # Пояснение и лучший вариант видны только в разборе после финала, во время игры — нет
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
    scene: Scene
    # Короткое описание момента решения — для разбора и аналитики
    situation: str | None = None
    dialogue: list[Line] = []
    timer_seconds: Annotated[int, Field(gt=0, le=300)] | None = None
    timeout_next: Id | None = None
    timeout_effects: Effects = Field(default_factory=Effects)
    # Что происходит, если проводник промедлил, — звучит перед следующей сценой
    timeout_reaction: list[Line] = []
    choices: list[Choice] = []
    outcome: Literal["success", "partial", "failure"] | None = None
    # Название финала: «Ситуация разрешена спокойно», «Потребовалось вмешательство начальника поезда»…
    ending: str | None = None
    final_text: str | None = None

    @model_validator(mode="after")
    def check_node_kind(self) -> "Node":
        if self.outcome is not None:
            if self.choices or self.timer_seconds is not None:
                raise ValueError("у финального узла (outcome) не может быть вариантов выбора и таймера")
            if not self.ending or not self.final_text:
                raise ValueError("у финального узла должны быть название финала ending и итоговый текст final_text")
            return self

        if not self.situation:
            raise ValueError("нужно короткое описание момента решения situation — оно показывается в разборе")
        if not self.dialogue:
            raise ValueError("нужен диалог (dialogue): хотя бы одна реплика перед выбором")
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
    # Номера ситуаций из справочника «Ситуации на борту», которые отрабатывает сценарий
    situations: list[int] = Field(min_length=1)
    initial: InitialScales
    # Скрытые параметры и их начальные значения, например trust: 50, tension: 30
    stats: dict[Name, ScaleValue] = {}
    characters: dict[Name, Character] = Field(min_length=1)
    start_node: Id
    nodes: list[Node] = Field(min_length=1)

    @field_validator("service_class")
    @classmethod
    def known_service_class(cls, value: str) -> str:
        titles = handbook.service_class_titles()
        if value not in titles:
            raise ValueError(f"класс обслуживания «{value}» не из справочника. Допустимые: {', '.join(titles)}")
        return value

    @field_validator("situations")
    @classmethod
    def known_situations(cls, value: list[int]) -> list[int]:
        unknown = [number for number in value if not 1 <= number <= len(handbook.situations)]
        if unknown:
            raise ValueError(f"в справочнике нет ситуаций с номерами {', '.join(map(str, unknown))}")
        return value

    @field_validator("characters")
    @classmethod
    def reserved_names(cls, value: dict[str, Character]) -> dict[str, Character]:
        if NARRATOR in value:
            raise ValueError("имя narrator зарезервировано за рассказчиком")
        return value
