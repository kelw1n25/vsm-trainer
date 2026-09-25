from pathlib import Path
from typing import Literal

import yaml
from pydantic import BaseModel, Field, model_validator

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "game.yaml"

Outcome = Literal["success", "partial", "failure"]


class TimerRules(BaseModel):
    answer_grace_seconds: float = Field(ge=0, le=5)


class ScaleWeights(BaseModel):
    loyalty: float = Field(ge=0)
    safety: float = Field(ge=0)


class FastAnswerRule(BaseModel):
    timer_fraction: float = Field(gt=0, le=1)
    competence: str
    points: int = Field(ge=0)


class ScoringRules(BaseModel):
    xp_by_outcome: dict[Outcome, int]
    difficulty_multiplier: dict[Literal[1, 2, 3], float]
    scale_weights: ScaleWeights
    fast_answer: FastAnswerRule


class GameConfig(BaseModel):
    competences: dict[str, str]
    timer: TimerRules
    scoring: ScoringRules

    @model_validator(mode="after")
    def complete_tables(self) -> "GameConfig":
        if set(self.scoring.xp_by_outcome) != {"success", "partial", "failure"}:
            raise ValueError("scoring.xp_by_outcome: нужны значения для success, partial и failure")
        if set(self.scoring.difficulty_multiplier) != {1, 2, 3}:
            raise ValueError("scoring.difficulty_multiplier: нужны значения для сложности 1, 2 и 3")
        if self.scoring.fast_answer.competence not in self.competences:
            raise ValueError("scoring.fast_answer.competence: неизвестная компетенция")
        return self


game_config = GameConfig.model_validate(yaml.safe_load(CONFIG_PATH.read_text(encoding="utf-8")))
