import logging
from fastapi import APIRouter, Depends, Request
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.auth import tokens
from app.auth.rate_limit import login_limiter
from app.auth.security import create_token, verify_password
from app.config import settings
from app.db import get_db
from app.errors import api_error
from app.profiles.models import Employee, Role

router = APIRouter(prefix="/api/auth", tags=["auth"])
audit = logging.getLogger("audit")


class LoginRequest(BaseModel):
    personnel_number: str = Field(min_length=1, max_length=20)
    password: str = Field(min_length=1, max_length=200)


class RefreshRequest(BaseModel):
    refresh_token: str = Field(min_length=1, max_length=200)


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    # Через сколько секунд access-токен истечёт; после этого клиент обменивает refresh_token
    expires_in: int
    refresh_token: str
    employee_id: int
    full_name: str
    role: Role


def _response(employee: Employee, refresh_token: str) -> LoginResponse:
    return LoginResponse(
        access_token=create_token(employee.id, employee.role),
        expires_in=settings.jwt_ttl_minutes * 60,
        refresh_token=refresh_token,
        employee_id=employee.id,
        full_name=employee.full_name,
        role=employee.role,
    )


@router.post("/login", summary="Вход по табельному номеру и паролю")
def login(body: LoginRequest, request: Request, db: Session = Depends(get_db)) -> LoginResponse:
    ip = request.client.host if request.client else "unknown"
    login_limiter.check(body.personnel_number, ip)
    employee = db.scalar(select(Employee).where(Employee.personnel_number == body.personnel_number))
    if employee is None or not verify_password(body.password, employee.password_hash):
        login_limiter.failed(body.personnel_number, ip)
        # Табельный номер в журнал не пишем: достаточно факта неудачи и адреса
        audit.info("login_failed", extra={"ip": ip})
        raise api_error(401, "invalid_credentials", "Неверный табельный номер или пароль")
    login_limiter.succeeded(body.personnel_number)
    audit.info("login", extra={"employee_id": employee.id, "ip": ip})
    refresh_token = tokens.issue(db, employee.id, db.scalar(select(func.now())))
    db.commit()
    return _response(employee, refresh_token)


@router.post("/refresh", summary="Обменять refresh-токен на новую пару токенов (ротация)")
def refresh(body: RefreshRequest, db: Session = Depends(get_db)) -> LoginResponse:
    now = db.scalar(select(func.now()))
    employee, refresh_token = tokens.rotate(db, body.refresh_token, now)
    db.commit()
    return _response(employee, refresh_token)


@router.post("/logout", summary="Выйти: отозвать refresh-токен этого устройства")
def logout(body: RefreshRequest, db: Session = Depends(get_db)) -> dict[str, str]:
    tokens.revoke(db, body.refresh_token, db.scalar(select(func.now())))
    db.commit()
    return {"status": "ok"}
