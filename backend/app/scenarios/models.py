from datetime import datetime

from sqlalchemy import DateTime, String, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base


class Scenario(Base):
    """Сценарий из файла в репозитории. Граф узлов хранится целиком в definition."""

    __tablename__ = "scenarios"

    # id берётся из файла сценария, например "business-seat-conflict"
    id: Mapped[str] = mapped_column(String(100), primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    category: Mapped[str] = mapped_column(String(30))
    difficulty: Mapped[int]
    service_class: Mapped[str] = mapped_column(String(50))
    route: Mapped[str] = mapped_column(String(100))
    definition: Mapped[dict] = mapped_column(JSONB)
    # Хеш содержимого файла: при старте перезаписываем сценарий, только если файл изменился
    content_hash: Mapped[str] = mapped_column(String(64))
    loaded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
