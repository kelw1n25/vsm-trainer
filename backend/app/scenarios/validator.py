"""Проверка файлов сценариев.

Запуск из командной строки (проверить все сценарии без перезапуска приложения):
    python -m app.scenarios.validator [файл.yaml ...]
"""

import sys
from collections import Counter, deque
from collections.abc import Iterator
from pathlib import Path
from typing import Any

import yaml
from pydantic import ValidationError

from app.scenarios.conditions import FlagCondition, parse_condition
from app.scenarios.schema import Node, ScenarioDefinition

SCENARIOS_DIR = Path(__file__).resolve().parents[2] / "scenarios"

_MESSAGES = {
    "missing": "обязательное поле не заполнено",
    "extra_forbidden": "неизвестное поле (опечатка в названии?)",
    "literal_error": "допустимые значения: {expected}",
    "string_type": "ожидается строка",
    "string_pattern_mismatch": "значение не соответствует формату {pattern}",
    "string_too_long": "не длиннее {max_length} символов",
    "string_too_short": "поле не может быть пустым",
    "int_type": "ожидается целое число",
    "int_parsing": "ожидается целое число",
    "bool_type": "ожидается true или false",
    "bool_parsing": "ожидается true или false",
    "list_type": "ожидается список",
    "dict_type": "ожидается набор полей",
    "model_type": "ожидается набор полей",
    "too_short": "список не может быть пустым",
    "greater_than": "значение должно быть больше {gt}",
    "greater_than_equal": "значение должно быть не меньше {ge}",
    "less_than_equal": "значение должно быть не больше {le}",
}


def validate_text(text: str, expected_id: str) -> tuple[ScenarioDefinition | None, list[str]]:
    """Возвращает сценарий и пустой список ошибок либо None и список понятных ошибок."""
    try:
        data = yaml.safe_load(text)
    except yaml.YAMLError as error:
        return None, [f"ошибка синтаксиса YAML: {error}"]
    if not isinstance(data, dict):
        return None, ["файл должен содержать описание сценария (набор полей id, title, nodes, ...)"]

    try:
        scenario = ScenarioDefinition.model_validate(data)
    except ValidationError as error:
        return None, [_format_error(item, data) for item in error.errors()]

    errors = []
    if scenario.id != expected_id:
        errors.append(f"id сценария «{scenario.id}» должен совпадать с именем файла «{expected_id}»")
    errors += validate_graph(scenario)
    return (None if errors else scenario), errors


def validate_file(path: Path) -> tuple[ScenarioDefinition | None, list[str]]:
    return validate_text(path.read_text(encoding="utf-8"), path.stem)


def validate_graph(scenario: ScenarioDefinition) -> list[str]:
    errors = []
    nodes = {node.id: node for node in scenario.nodes}

    duplicates = [nid for nid, count in Counter(n.id for n in scenario.nodes).items() if count > 1]
    if duplicates:
        errors.append(f"повторяются id узлов: {', '.join(duplicates)}")
    if scenario.start_node not in nodes:
        errors.append(f"start_node ссылается на несуществующий узел «{scenario.start_node}»")

    for node in scenario.nodes:
        for target, source in _edges(node):
            if target not in nodes:
                errors.append(f"узел «{node.id}», {source}: переход в несуществующий узел «{target}»")

    reachable = _walk({scenario.start_node}, lambda nid: [t for t, _ in _edges(nodes[nid])], nodes)
    unreachable = [node.id for node in scenario.nodes if node.id not in reachable]
    if unreachable:
        errors.append(f"недостижимые узлы (в них нет переходов от start_node): {', '.join(unreachable)}")

    finals = {node.id for node in scenario.nodes if node.outcome is not None}
    if not finals:
        errors.append("в сценарии нет ни одного финального узла (outcome)")
    predecessors: dict[str, list[str]] = {nid: [] for nid in nodes}
    for node in scenario.nodes:
        for target, _ in _edges(node):
            if target in predecessors:
                predecessors[target].append(node.id)
    can_finish = _walk(finals, lambda nid: predecessors[nid], nodes)
    stuck = [nid for nid in nodes if nid in reachable and nid not in can_finish]
    if stuck and finals:
        errors.append(f"из узлов нельзя дойти ни до одного финала (зацикливание): {', '.join(stuck)}")

    set_flags = {
        flag
        for node in scenario.nodes
        for effects in [node.timeout_effects, *(choice.effects for choice in node.choices)]
        for flag in effects.set_flags
    }
    for node in scenario.nodes:
        for choice in node.choices:
            for text in choice.condition:
                condition = parse_condition(text)
                if isinstance(condition, FlagCondition) and condition.flag not in set_flags:
                    errors.append(
                        f"узел «{node.id}», вариант «{choice.id}»: условие «{text}» ссылается на флаг, "
                        "который нигде в сценарии не устанавливается (set_flags)"
                    )
    return errors


def _edges(node: Node) -> Iterator[tuple[str, str]]:
    for choice in node.choices:
        yield choice.next, f"вариант «{choice.id}»"
    if node.timeout_next is not None:
        yield node.timeout_next, "timeout_next"


def _walk(start: set[str], neighbours, nodes: dict[str, Node]) -> set[str]:
    """Обход в ширину: все узлы, достижимые из start (несуществующие узлы пропускаются)."""
    seen = {nid for nid in start if nid in nodes}
    queue = deque(seen)
    while queue:
        for nid in neighbours(queue.popleft()):
            if nid in nodes and nid not in seen:
                seen.add(nid)
                queue.append(nid)
    return seen


def _format_error(error: dict[str, Any], data: dict[str, Any]) -> str:
    template = _MESSAGES.get(error["type"])
    if error["type"] == "value_error":
        message = str(error["ctx"]["error"])
    elif template:
        message = template.format(**error.get("ctx", {}))
    else:
        message = error["msg"]
    return f"{_describe_location(error['loc'], data)}: {message}"


def _describe_location(loc: tuple, data: Any) -> str:
    """Превращает ('nodes', 3, 'choices', 0, 'next') в «узел «escalation», вариант «call_chief», поле next»."""
    parts = []
    labels = {"nodes": "узел", "choices": "вариант"}
    current = data
    for index, key in enumerate(loc):
        previous = loc[index - 1] if index else None
        if isinstance(key, int) and previous in labels:
            item = current[key] if isinstance(current, list) and key < len(current) else None
            name = item.get("id") if isinstance(item, dict) else None
            parts[-1] = f"{labels[previous]} «{name}»" if name else f"{labels[previous]} №{key + 1}"
        else:
            parts.append(f"поле {key}")
        current = current.get(key) if isinstance(current, dict) else (
            current[key] if isinstance(current, list) and isinstance(key, int) and key < len(current) else None
        )
    return ", ".join(parts) or "сценарий"


def main(paths: list[Path]) -> int:
    failed = 0
    for path in paths:
        _, errors = validate_file(path)
        if errors:
            failed += 1
            print(f"✗ {path.name}")
            for error in errors:
                print(f"    - {error}")
        else:
            print(f"✓ {path.name}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main([Path(arg) for arg in sys.argv[1:]] or sorted(SCENARIOS_DIR.glob("*.yaml"))))
