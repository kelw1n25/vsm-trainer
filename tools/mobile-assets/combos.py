"""Какие сочетания персонажа нужны мобильным приложениям.

Персонаж новеллы на сайте — SVG-компонент Person: форма (сотрудник/пассажир), поза, эмоция, жест, предмет,
сторона сцены. Мобильные приложения показывают те же фигуры картинками, экспортированными из этого компонента.
Скрипт обходит все сценарии и собирает сочетания, которые реально встречаются, плюс базовый набор
«поза × эмоция» — им приложение подменяет сочетание, которого нет в наборе (например, в новом сценарии).

Запуск: python tools/mobile-assets/combos.py > frontend/tools/sprites.json
"""

import glob
import json
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
# Как на сайте (StoryStage.HANDS): жест по умолчанию для эмоции
HANDS = {"thinking": "throat", "pained": "chest", "worried": "chest"}
EXPRESSIONS = ["neutral", "happy", "angry", "annoyed", "worried", "sad", "surprised", "thinking", "serious", "pained", "tipsy"]
POSES = ["stand", "sit", "wheelchair"]


def sprite(uniform: bool, pose: str, expression: str, hand: str | None, item: str | None, right: bool) -> dict:
    hand = hand or HANDS.get(expression, "down")
    name = f"sprite_{'u' if uniform else 'p'}_{pose}_{expression}_{hand}_{item or 'none'}_{'r' if right else 'l'}"
    return {"name": name, "uniform": uniform, "pose": pose, "expression": expression, "hand": hand, "item": item, "right": right}


def main() -> None:
    found: dict[str, dict] = {}
    for path in sorted(glob.glob(str(ROOT / "backend/scenarios/*.yaml"))):
        scenario = yaml.safe_load(open(path, encoding="utf-8"))
        cast = scenario.get("characters", {})

        def uniform(character_id: str) -> bool:
            return character_id == "player" or (cast.get(character_id, {}).get("look") or {}).get("outfit") == "uniform"

        for node in scenario["nodes"]:
            lines = list(node.get("dialogue") or [])
            for choice in node.get("choices") or []:
                lines += choice.get("reaction") or []
            lines += node.get("timeout_reaction") or []
            for character in (node.get("scene") or {}).get("characters", []):
                expressions = {character.get("expression", "neutral")}
                expressions |= {line["expression"] for line in lines if line.get("speaker") == character["id"] and line.get("expression")}
                for expression in expressions:
                    entry = sprite(uniform(character["id"]), character.get("pose", "stand"), expression,
                                   character.get("hand"), character.get("item"), character.get("position") == "right")
                    found[entry["name"]] = entry
    # Базовый набор: без предмета, жест по эмоции, смотрит вправо (слева на сцене); справа — зеркально
    for uniform_flag in (True, False):
        for pose in POSES:
            for expression in EXPRESSIONS:
                entry = sprite(uniform_flag, pose, expression, None, None, False)
                found.setdefault(entry["name"], entry)
    json.dump(sorted(found.values(), key=lambda s: s["name"]), sys.stdout, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
