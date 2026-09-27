from datetime import datetime
from enum import StrEnum

from sqlalchemy import DateTime, Enum, ForeignKey, LargeBinary, String, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base, CreatedAtMixin


class Role(StrEnum):
    CONDUCTOR = "conductor"
    INSTRUCTOR = "instructor"


class Depot(Base):
    __tablename__ = "depots"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100), unique=True)

    brigades: Mapped[list["Brigade"]] = relationship(back_populates="depot")


class Brigade(Base):
    __tablename__ = "brigades"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(100))
    depot_id: Mapped[int] = mapped_column(ForeignKey("depots.id"))

    depot: Mapped[Depot] = relationship(back_populates="brigades")
    employees: Mapped[list["Employee"]] = relationship(back_populates="brigade")


class Employee(CreatedAtMixin, Base):
    __tablename__ = "employees"

    id: Mapped[int] = mapped_column(primary_key=True)
    full_name: Mapped[str] = mapped_column(String(200))
    personnel_number: Mapped[str] = mapped_column(String(20), unique=True)
    password_hash: Mapped[str] = mapped_column(String(200))
    role: Mapped[Role] = mapped_column(
        Enum(Role, native_enum=False, length=20, values_callable=lambda e: [m.value for m in e]),
        default=Role.CONDUCTOR,
    )
    brigade_id: Mapped[int] = mapped_column(ForeignKey("brigades.id"))
    xp: Mapped[int] = mapped_column(default=0)
    # {"communication": 12, "first_aid": 5, ...} — список компетенций задаётся в конфиге
    competence_points: Mapped[dict[str, int]] = mapped_column(JSONB, default=dict)
    # Аватар-конструктор {"background", "headwear", "tie"} — см. profiles/avatar.py; пусто — по умолчанию
    avatar: Mapped[dict[str, str]] = mapped_column(JSONB, default=dict, server_default="{}")
    # Сгорание баллов: дни без прохождений считаются от более поздней из двух дат
    last_activity_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    last_burn_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))

    brigade: Mapped[Brigade] = relationship(back_populates="employees")


class EmployeePhoto(Base):
    """Своё фото на аватаре — только с согласия сотрудника, отдельно от строки сотрудника.

    Хранится уже обработанный квадрат JPEG без метаданных (см. profiles/photo.py); удаление фото стирает строку.
    """

    __tablename__ = "employee_photos"

    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id", ondelete="CASCADE"), primary_key=True)
    image: Mapped[bytes] = mapped_column(LargeBinary)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
