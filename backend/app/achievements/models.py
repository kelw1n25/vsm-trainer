import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base


class EmployeeAchievement(Base):
    """Полученная ачивка. Сами правила ачивок лежат в конфиге, здесь только факт получения."""

    __tablename__ = "employee_achievements"
    __table_args__ = (UniqueConstraint("employee_id", "code"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id"))
    code: Mapped[str] = mapped_column(String(50))
    run_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("scenario_runs.id"))
    earned_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
