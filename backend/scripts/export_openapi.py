"""Выгружает OpenAPI-схему backend в docs/openapi.yaml.

Запуск из каталога backend: python scripts/export_openapi.py
Тест tests/test_openapi.py падает, если файл разошёлся с кодом, — значит, пора перезапустить скрипт.
"""

import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.main import app  # noqa: E402

TARGET = Path(__file__).resolve().parents[2] / "docs" / "openapi.yaml"


def render() -> str:
    return yaml.safe_dump(app.openapi(), allow_unicode=True, sort_keys=False, width=120)


if __name__ == "__main__":
    TARGET.write_text(render(), encoding="utf-8")
    print(f"OpenAPI записан в {TARGET}")
