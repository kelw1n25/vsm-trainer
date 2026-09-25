from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, String, Text, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base, CreatedAtMixin


class Notification(CreatedAtMixin, Base):
    __tablename__ = "notifications"
    __table_args__ = (
        # dedup_key не даёт создать одно и то же уведомление дважды (например, челлендж одной недели)
        UniqueConstraint("employee_id", "dedup_key"),
        Index("ix_notifications_employee_created", "employee_id", "created_at"),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id"))
    type: Mapped[str] = mapped_column(String(30))
    title: Mapped[str] = mapped_column(String(200))
    body: Mapped[str] = mapped_column(Text)
    dedup_key: Mapped[str] = mapped_column(String(100))
    read_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
