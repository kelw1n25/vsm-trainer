from pathlib import Path
from typing import Annotated, Literal

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


class PointsBurnRules(BaseModel):
    inactive_days: int = Field(ge=1)
    burn_fraction: float = Field(gt=0, lt=1)
    warn_days_before: int = Field(ge=0)


class WeeklyChallenge(BaseModel):
    scenario_id: str
    bonus_xp: int = Field(ge=0)


class Level(BaseModel):
    level: int
    title: str
    xp: int = Field(ge=0)


class FastStreakRule(BaseModel):
    type: Literal["fast_streak"]
    count: int = Field(ge=1)
    timer_fraction: float = Field(gt=0, le=1)


class ScenarioResultRule(BaseModel):
    type: Literal["scenario_result"]
    category: str | None = None
    outcomes: list[Outcome] = ["success", "partial"]
    loyalty_above: int | None = None
    safety_above: int | None = None
    no_timeouts: bool = False
    scales_never_below: int | None = None


class RunsCompletedRule(BaseModel):
    type: Literal["runs_completed"]
    count: int = Field(ge=1)


class CategoriesCoveredRule(BaseModel):
    type: Literal["categories_covered"]
    count: int = Field(ge=1)


class CompetenceTotalRule(BaseModel):
    type: Literal["competence_total"]
    competence: str
    points: int = Field(ge=1)


AchievementRule = FastStreakRule | ScenarioResultRule | RunsCompletedRule | CategoriesCoveredRule | CompetenceTotalRule


class Achievement(BaseModel):
    title: str
    description: str
    rule: Annotated[AchievementRule, Field(discriminator="type")]


class GameConfig(BaseModel):
    competences: dict[str, str]
    timer: TimerRules
    scoring: ScoringRules
    points_burn: PointsBurnRules
    weekly_challenge: WeeklyChallenge | None = None
    levels: list[Level] = Field(min_length=1)
    achievements: dict[str, Achievement]

    @model_validator(mode="after")
    def complete_tables(self) -> "GameConfig":
        thresholds = [level.xp for level in self.levels]
        if thresholds[0] != 0 or thresholds != sorted(set(thresholds)):
            raise ValueError("levels: пороги XP должны начинаться с 0 и строго возрастать")
        for code, achievement in self.achievements.items():
            rule = achievement.rule
            if isinstance(rule, CompetenceTotalRule) and rule.competence not in self.competences:
                raise ValueError(f"achievements.{code}: неизвестная компетенция {rule.competence}")
        if self.points_burn.warn_days_before >= self.points_burn.inactive_days:
            raise ValueError("points_burn.warn_days_before должно быть меньше inactive_days")
        if set(self.scoring.xp_by_outcome) != {"success", "partial", "failure"}:
            raise ValueError("scoring.xp_by_outcome: нужны значения для success, partial и failure")
        if set(self.scoring.difficulty_multiplier) != {1, 2, 3}:
            raise ValueError("scoring.difficulty_multiplier: нужны значения для сложности 1, 2 и 3")
        if self.scoring.fast_answer.competence not in self.competences:
            raise ValueError("scoring.fast_answer.competence: неизвестная компетенция")
        return self


game_config = GameConfig.model_validate(yaml.safe_load(CONFIG_PATH.read_text(encoding="utf-8")))
