import re
from dataclasses import dataclass

_SCALE_RE = re.compile(r"^(loyalty|safety)\s*(>=|<=|>|<|==)\s*(\d+)$")
_FLAG_RE = re.compile(r"^(not\s+)?flag\s+(\w+)$")


@dataclass(frozen=True)
class ScaleCondition:
    scale: str
    op: str
    value: int


@dataclass(frozen=True)
class FlagCondition:
    flag: str
    present: bool


Condition = ScaleCondition | FlagCondition


def parse_condition(text: str) -> Condition:
    text = text.strip()
    if match := _SCALE_RE.match(text):
        return ScaleCondition(scale=match[1], op=match[2], value=int(match[3]))
    if match := _FLAG_RE.match(text):
        return FlagCondition(flag=match[2], present=match[1] is None)
    raise ValueError(
        f"не удалось разобрать условие «{text}». "
        "Примеры: «safety >= 40», «loyalty < 30», «flag medic_called», «not flag medic_called»"
    )
