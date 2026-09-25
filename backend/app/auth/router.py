from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.security import create_token, verify_password
from app.db import get_db
from app.errors import api_error
from app.profiles.models import Employee, Role

router = APIRouter(prefix="/api/auth", tags=["auth"])


class LoginRequest(BaseModel):
    personnel_number: str = Field(min_length=1, max_length=20)
    password: str = Field(min_length=1, max_length=200)


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    employee_id: int
    full_name: str
    role: Role


@router.post("/login", summary="Вход по табельному номеру и паролю")
def login(body: LoginRequest, db: Session = Depends(get_db)) -> LoginResponse:
    employee = db.scalar(select(Employee).where(Employee.personnel_number == body.personnel_number))
    if employee is None or not verify_password(body.password, employee.password_hash):
        raise api_error(401, "invalid_credentials", "Неверный табельный номер или пароль")
    return LoginResponse(
        access_token=create_token(employee.id, employee.role),
        employee_id=employee.id,
        full_name=employee.full_name,
        role=employee.role,
    )
