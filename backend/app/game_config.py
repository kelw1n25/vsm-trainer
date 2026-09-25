from pathlib import Path

import yaml
from pydantic import BaseModel

CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "game.yaml"


class GameConfig(BaseModel):
    competences: dict[str, str]


game_config = GameConfig.model_validate(yaml.safe_load(CONFIG_PATH.read_text(encoding="utf-8")))
