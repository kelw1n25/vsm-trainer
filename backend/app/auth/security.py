import hashlib
import hmac
import secrets
from datetime import UTC, datetime, timedelta

import jwt

from app.config import settings

_SCRYPT = {"n": 2**14, "r": 8, "p": 1}


def hash_password(password: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.scrypt(password.encode(), salt=salt, **_SCRYPT)
    return f"scrypt${salt.hex()}${digest.hex()}"


def verify_password(password: str, stored_hash: str) -> bool:
    try:
        _, salt, digest = stored_hash.split("$")
    except ValueError:
        # Пустой хеш: у сотрудника нет пароля (создан через HR API), вход по паролю закрыт
        return False
    candidate = hashlib.scrypt(password.encode(), salt=bytes.fromhex(salt), **_SCRYPT)
    return hmac.compare_digest(candidate.hex(), digest)


def create_token(employee_id: int, role: str) -> str:
    expires = datetime.now(UTC) + timedelta(minutes=settings.jwt_ttl_minutes)
    return jwt.encode({"sub": str(employee_id), "role": role, "exp": expires}, settings.jwt_secret, "HS256")


def decode_token(token: str) -> int:
    """Возвращает id сотрудника. Бросает jwt.InvalidTokenError, если токен подделан или истёк."""
    return int(jwt.decode(token, settings.jwt_secret, algorithms=["HS256"])["sub"])
