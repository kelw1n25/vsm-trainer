import uuid

from sqlalchemy import ForeignKey, Index, String
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base, CreatedAtMixin


class Event(CreatedAtMixin, Base):
    """Журнал действий пользователя: выборы, таймауты, старты и финалы сценариев.

    Из него строятся разбор после сценария и аналитика компетенций.
    """

    __tablename__ = "events"
    __table_args__ = (
        Index("ix_events_employee_created", "employee_id", "created_at"),
        Index("ix_events_run", "run_id"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id"))
    run_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("scenario_runs.id"))
    type: Mapped[str] = mapped_column(String(50))
    payload: Mapped[dict] = mapped_column(JSONB, default=dict)
