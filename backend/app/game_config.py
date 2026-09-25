from pathlib import Path

import yaml
from pydantic import BaseModel, Field

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "game.yaml"


class TimerRules(BaseModel):
    answer_grace_seconds: float = Field(ge=0, le=5)


class GameConfig(BaseModel):
    competences: dict[str, str]
    timer: TimerRules


game_config = GameConfig.model_validate(yaml.safe_load(CONFIG_PATH.read_text(encoding="utf-8")))
