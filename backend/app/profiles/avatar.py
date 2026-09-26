"""Аватар сотрудника — конструктор из деталей формы, а не фотография.

Фото сотрудника — персональные данные (152-ФЗ), поэтому аватар собирается из готовых вариантов:
фон, головной убор, галстук. Сервер принимает только значения из списков ниже — клиенты лишь рисуют выбор.
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict

Background = Literal["blue", "mint", "sand", "lilac", "coral", "night"]
Headwear = Literal["cap", "none"]
Tie = Literal["red", "blue", "green", "graphite"]


class Avatar(BaseModel):
    model_config = ConfigDict(extra="forbid")

    background: Background = "blue"
    headwear: Headwear = "cap"
    tie: Tie = "red"


def avatar_of(stored: dict | None) -> Avatar:
    """Сохранённый выбор; пустой — аватар по умолчанию, как у всех до настройки."""
    return Avatar.model_validate(stored or {})
