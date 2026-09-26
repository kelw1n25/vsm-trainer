import hashlib
import logging
import secrets
from datetime import datetime, timedelta

from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.auth.models import RefreshToken
from app.config import settings
from app.errors import api_error
from app.profiles.models import Employee

audit = logging.getLogger("audit")


def _digest(raw: str) -> str:
    return hashlib.sha256(raw.encode()).hexdigest()


def issue(db: Session, employee_id: int, now: datetime) -> str:
    """Новый refresh-токен. Клиент получает сырое значение один раз, в базе остаётся только хеш."""
    raw = secrets.token_urlsafe(32)
    db.add(RefreshToken(
        employee_id=employee_id,
        token_hash=_digest(raw),
        expires_at=now + timedelta(days=settings.refresh_ttl_days),
    ))
    return raw


def rotate(db: Session, raw: str, now: datetime) -> tuple[Employee, str]:
    """Обменивает refresh-токен на новый. Старый отзывается, повторно его предъявить нельзя."""
    token = db.scalar(select(RefreshToken).where(RefreshToken.token_hash == _digest(raw)).with_for_update())
    if token is None or token.expires_at <= now:
        raise api_error(401, "invalid_refresh_token", "Сессия истекла, войдите заново")
    if token.revoke_reason == "rotated":
        # Обменянный токен предъявили ещё раз — его скопировали. Закрываем все сессии сотрудника
        revoke_all(db, token.employee_id, now, "reuse")
        db.commit()
        audit.warning("refresh_reuse", extra={"employee_id": token.employee_id})
        raise api_error(401, "invalid_refresh_token", "Сессия завершена из соображений безопасности, войдите заново")
    if token.revoked_at is not None:
        raise api_error(401, "invalid_refresh_token", "Сессия завершена, войдите заново")
    token.revoked_at, token.revoke_reason = now, "rotated"
    employee = db.get(Employee, token.employee_id)
    return employee, issue(db, employee.id, now)


def revoke(db: Session, raw: str, now: datetime) -> None:
    db.execute(
        update(RefreshToken)
        .where(RefreshToken.token_hash == _digest(raw), RefreshToken.revoked_at.is_(None))
        .values(revoked_at=now, revoke_reason="logout")
    )


def revoke_all(db: Session, employee_id: int, now: datetime, reason: str) -> None:
    db.execute(
        update(RefreshToken)
        .where(RefreshToken.employee_id == employee_id, RefreshToken.revoked_at.is_(None))
        .values(revoked_at=now, revoke_reason=reason)
    )
