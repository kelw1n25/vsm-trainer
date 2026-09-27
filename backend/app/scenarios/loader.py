import hashlib
import json
import logging
from pathlib import Path

from sqlalchemy import func
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.scenarios.models import Scenario
from app.scenarios.validator import SCENARIOS_DIR, validate_text

logger = logging.getLogger(__name__)


def load_scenarios(db: Session, directory: Path = SCENARIOS_DIR) -> None:
    """Загружает сценарии из файлов в БД.

    Файл с ошибками пропускается с понятным сообщением в логе: в БД остаётся
    его последняя корректная версия, поэтому опечатка не ломает работающую демонстрацию.
    """
    for path in sorted(directory.glob("*.yaml")):
        scenario, errors = validate_text(path.read_text(encoding="utf-8"), path.stem)
        if errors:
            logger.error("Сценарий %s не загружен:\n  - %s", path.name, "\n  - ".join(errors))
            continue

        definition = scenario.model_dump(mode="json")
        values = {
            "title": scenario.title,
            "category": scenario.category,
            "difficulty": scenario.difficulty,
            "service_class": scenario.service_class,
            "route": scenario.route,
            "definition": definition,
            # Хэш того, что хранится, а не байтов файла: новое правило разбора (например, обрезка
            # пробелов) обновит сценарии в БД и без правки самих файлов
            "content_hash": hashlib.sha256(json.dumps(definition, sort_keys=True, ensure_ascii=False).encode()).hexdigest(),
        }
        # ON CONFLICT: несколько копий backend могут стартовать одновременно
        statement = (
            insert(Scenario)
            .values(id=scenario.id, **values)
            .on_conflict_do_update(
                index_elements=[Scenario.id],
                set_={**values, "loaded_at": func.now()},
                where=Scenario.content_hash != values["content_hash"],
            )
        )
        if db.execute(statement).rowcount:
            logger.info("Сценарий %s загружен", path.name)
        else:
            logger.info("Сценарий %s не изменился", path.name)
    db.commit()
