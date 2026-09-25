import uuid
from datetime import datetime
from enum import StrEnum

from sqlalchemy import DateTime, Enum, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base


class RunStatus(StrEnum):
    IN_PROGRESS = "in_progress"
    SUCCESS = "success"
    PARTIAL = "partial"
    FAILURE = "failure"


class ScenarioRun(Base):
    """Прохождение сценария. Хранит всё состояние, поэтому backend остаётся stateless."""

    __tablename__ = "scenario_runs"
    __table_args__ = (Index("ix_scenario_runs_employee_finished", "employee_id", "finished_at"),)

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id"))
    scenario_id: Mapped[str] = mapped_column(ForeignKey("scenarios.id"))
    status: Mapped[RunStatus] = mapped_column(
        Enum(RunStatus, native_enum=False, length=20, values_callable=lambda e: [m.value for m in e]),
        default=RunStatus.IN_PROGRESS,
    )
    current_node_id: Mapped[str] = mapped_column(String(100))
    loyalty: Mapped[int]
    safety: Mapped[int]
    flags: Mapped[list[str]] = mapped_column(JSONB, default=list)
    # Скрытые параметры сценария (доверие, напряжение…): влияют на ветки, игроку не показываются
    stats: Mapped[dict[str, int]] = mapped_column(JSONB, default=dict)
    # Серверная метка входа в текущий узел
    node_entered_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    # Когда проводник дочитал сцену и увидел варианты — от этой метки считается таймер.
    # Пока варианты не показаны, время на чтение диалога не идёт в зачёт решения
    choices_shown_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # "final" — дошёл до финального узла; "loyalty_depleted" / "safety_depleted" — шкала упала до нуля
    finish_reason: Mapped[str | None] = mapped_column(String(30))
    xp_earned: Mapped[int] = mapped_column(default=0)
    competence_points: Mapped[dict[str, int]] = mapped_column(JSONB, default=dict)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
