"""Справочник проводника из материалов кейсодержателя: ситуации на борту, ролевая модель, классы обслуживания.

Файл config/handbook.yaml читается один раз при старте, как и правила игры. Ошибка в нём останавливает
запуск с понятным сообщением, а не ломает страницы по одной.
"""

from pathlib import Path
from typing import Literal

import yaml
from pydantic import BaseModel, ConfigDict, Field, model_validator

HANDBOOK_PATH = Path(__file__).resolve().parents[2] / "config" / "handbook.yaml"


class Content(BaseModel):
    # Блоки YAML «>» оставляют перевод строки в конце — на телефоне это лишняя пустая строка в карточке
    model_config = ConfigDict(str_strip_whitespace=True)


class RoleModelStep(Content):
    code: str
    title: str
    phrases: list[str] = Field(min_length=1)


class RoleModel(Content):
    title: str
    steps: list[RoleModelStep] = Field(min_length=1)


class ServiceClass(Content):
    code: Literal["first", "business", "comfort", "standard"]
    title: str
    layout: str
    aisle_mm: int
    pitch_mm: int
    seat_mm: int
    max_wait_minutes: int
    summary: str


class Standard(Content):
    title: str
    text: str


class Situation(Content):
    number: int = Field(ge=1)
    title: str
    # boarding — на посадке, onboard — в пути следования
    stage: Literal["boarding", "onboard"]
    category: Literal["conflict", "medical", "service", "safety"]
    reaction: str
    phrases: list[str] = Field(min_length=1)
    comment: list[str] = Field(min_length=1)


class Handbook(Content):
    role_model: RoleModel
    service_classes: list[ServiceClass]
    standards: list[Standard]
    situations: list[Situation]

    @model_validator(mode="after")
    def numbered_in_order(self) -> "Handbook":
        numbers = [situation.number for situation in self.situations]
        if numbers != list(range(1, len(numbers) + 1)):
            raise ValueError("situations: номера ситуаций должны идти по порядку с 1 без пропусков")
        return self

    def situation(self, number: int) -> Situation:
        return self.situations[number - 1]

    def service_class_titles(self) -> list[str]:
        return [service_class.title for service_class in self.service_classes]


handbook = Handbook.model_validate(yaml.safe_load(HANDBOOK_PATH.read_text(encoding="utf-8")))
