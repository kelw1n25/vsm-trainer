import jwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.auth.security import decode_token
from app.db import get_db
from app.errors import api_error
from app.profiles.models import Employee

_bearer = HTTPBearer(auto_error=False)


def get_current_employee(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
    db: Session = Depends(get_db),
) -> Employee:
    if credentials is None:
        raise api_error(401, "not_authenticated", "Требуется вход в систему")
    try:
        employee_id = decode_token(credentials.credentials)
    except jwt.InvalidTokenError:
        raise api_error(401, "invalid_token", "Сессия истекла или недействительна, войдите заново")
    employee = db.get(Employee, employee_id)
    if employee is None:
        raise api_error(401, "invalid_token", "Сотрудник не найден, войдите заново")
    return employee
