import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db import Base, CreatedAtMixin


class RefreshToken(CreatedAtMixin, Base):
    """Долгоживущий токен мобильного клиента: обменивается на новую пару access + refresh.

    В базе лежит только SHA-256 от токена — утечка таблицы не даёт войти. Каждый обмен отзывает
    использованный токен (ротация); повторное предъявление уже обменянного токена значит, что его
    скопировали, и тогда отзываются все сессии сотрудника.
    """

    __tablename__ = "refresh_tokens"
    __table_args__ = (Index("ix_refresh_tokens_employee", "employee_id"),)

    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    employee_id: Mapped[int] = mapped_column(ForeignKey("employees.id"))
    token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # rotated — обменян на новый (повтор = кража), logout / reuse — сессия закрыта
    revoke_reason: Mapped[str | None] = mapped_column(String(20))
