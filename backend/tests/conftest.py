"""Интеграционные тесты работают с настоящим PostgreSQL.

Запуск: POSTGRES_* указывают на отдельную базу с именем *_test — её таблицы очищаются перед каждым тестом.
Без такой базы интеграционные тесты пропускаются, тесты валидатора работают всегда.
"""

import os

import pytest

os.environ.setdefault("JWT_SECRET", "test-secret-at-least-32-characters-long")
os.environ.setdefault("INTEGRATION_API_KEY", "test-integration-key-at-least-32-chars")


@pytest.fixture(autouse=True)
def no_weekly_challenge(monkeypatch):
    """Челлендж недели меняет XP за сценарий — по умолчанию выключен, тесты челленджа включают его сами."""
    from app.game_config import game_config

    monkeypatch.setattr(game_config, "weekly_challenge", None)


@pytest.fixture(autouse=True)
def fresh_login_limiter():
    """Счётчик неудачных входов живёт в памяти процесса — каждый тест начинает с чистого."""
    from app.auth.rate_limit import login_limiter

    login_limiter.reset()


@pytest.fixture(scope="session")
def database():
    if not os.environ.get("POSTGRES_DB", "").endswith("_test"):
        pytest.skip("нужна тестовая база: POSTGRES_DB=<имя>_test и остальные POSTGRES_*")
    from alembic import command
    from alembic.config import Config

    command.upgrade(Config("alembic.ini"), "head")


@pytest.fixture
def db(database):
    from sqlalchemy import text

    import app.main  # noqa: F401 — регистрирует все модели в Base.metadata
    from app.db import Base, SessionLocal
    from app.scenarios.loader import load_scenarios

    tables = ", ".join(table.name for table in Base.metadata.sorted_tables)
    with SessionLocal() as session:
        session.execute(text(f"TRUNCATE {tables} RESTART IDENTITY CASCADE"))
        session.commit()
        load_scenarios(session)
        yield session


@pytest.fixture
def client(db):
    from fastapi.testclient import TestClient

    from app.main import app

    return TestClient(app)


def create_employee(db, personnel_number: str = "100001", role: str = "conductor"):
    from app.auth.security import hash_password
    from app.profiles.models import Brigade, Depot, Employee

    depot = Depot(name=f"Депо {personnel_number}")
    brigade = Brigade(name="Бригада 1", depot=depot)
    employee = Employee(
        full_name="Тестовый Проводник",
        personnel_number=personnel_number,
        password_hash=hash_password("secret"),
        role=role,
        brigade=brigade,
    )
    db.add(employee)
    db.commit()
    return employee


def login(client, personnel_number: str) -> dict[str, str]:
    response = client.post("/api/auth/login", json={"personnel_number": personnel_number, "password": "secret"})
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


@pytest.fixture
def employee(db):
    return create_employee(db)


@pytest.fixture
def auth(client, employee) -> dict[str, str]:
    return login(client, employee.personnel_number)
