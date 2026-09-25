from datetime import timedelta

import pytest
from sqlalchemy import func, select, update

from tests.test_engine import BEST_PATH, PARTIAL_PATH, play


def days_ago(db, employee, days: float) -> None:
    from app.profiles.models import Employee

    db.execute(
        update(Employee)
        .where(Employee.id == employee.id)
        .values(last_activity_at=func.now() - timedelta(days=days))
    )
    db.commit()


def notifications(client, auth) -> dict:
    response = client.get("/api/notifications", headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def types(data: dict) -> list[str]:
    return [item["type"] for item in data["items"]]


def test_points_burn_for_each_inactive_period(client, auth, db, employee):
    employee.xp = 1000
    db.commit()
    days_ago(db, employee, 15)  # два полных периода по 7 дней

    data = notifications(client, auth)
    db.refresh(employee)
    assert employee.xp == 810  # 1000 → 900 → 810
    assert "points_burned" in types(data)

    # Повторный запрос не сжигает ещё раз: следующий период ещё не наступил
    notifications(client, auth)
    db.refresh(employee)
    assert employee.xp == 810


def test_no_burn_for_active_or_new_employee(client, auth, db, employee):
    employee.xp = 1000
    db.commit()
    notifications(client, auth)  # новый сотрудник: ещё не проходил сценарии
    days_ago(db, employee, 3)
    notifications(client, auth)
    db.refresh(employee)
    assert employee.xp == 1000


def test_burn_warning_is_sent_once(client, auth, db, employee):
    days_ago(db, employee, 5.5)  # до сгорания меньше 2 дней
    notifications(client, auth)
    data = notifications(client, auth)
    assert types(data).count("burn_warning") == 1


def test_scenario_resets_inactivity(client, auth, db, employee):
    employee.xp = 1000
    db.commit()
    days_ago(db, employee, 6.9)
    play(client, auth, *BEST_PATH)
    notifications(client, auth)
    db.refresh(employee)
    assert employee.xp == 1208


def test_leaderboard_applies_burn(client, auth, db, employee):
    employee.xp = 1000
    db.commit()
    days_ago(db, employee, 8)
    rows = client.get("/api/leaderboard?period=all", headers=auth).json()["rows"]
    assert rows[0]["points"] == 900


def test_achievement_and_new_scenario_notifications(client, auth):
    assert "new_scenario" in types(notifications(client, auth))
    play(client, auth, *BEST_PATH)
    data = notifications(client, auth)
    assert types(data).count("achievement") == 3
    titles = [item["title"] for item in data["items"]]
    assert "Новая ачивка: Дипломат" in titles


def test_mark_read(client, auth):
    data = notifications(client, auth)
    assert data["unread"] >= 1
    first = data["items"][0]["id"]
    assert client.post(f"/api/notifications/{first}/read", headers=auth).status_code == 200
    assert notifications(client, auth)["unread"] == data["unread"] - 1
    client.post("/api/notifications/read-all", headers=auth)
    assert notifications(client, auth)["unread"] == 0
    assert client.post("/api/notifications/999999/read", headers=auth).status_code == 404


@pytest.fixture
def weekly_challenge(monkeypatch):
    from app.game_config import WeeklyChallenge, game_config

    monkeypatch.setattr(game_config, "weekly_challenge", WeeklyChallenge(scenario_id="business-seat-conflict", bonus_xp=50))


def test_weekly_challenge_bonus_once_per_week(client, auth, db, weekly_challenge):
    from app.analytics.models import Event

    assert "weekly_challenge" in types(notifications(client, auth))
    first = play(client, auth, *BEST_PATH)
    second = play(client, auth, *BEST_PATH)
    assert (first["final"]["xp_earned"], second["final"]["xp_earned"]) == (258, 208)
    assert db.scalar(select(func.count()).where(Event.type == "challenge_completed")) == 1


def test_weekly_challenge_requires_success(client, auth, weekly_challenge):
    state = play(client, auth, *PARTIAL_PATH)
    assert state["final"]["xp_earned"] == 124  # 50 × 1.5 + 70 × 0.3 + 95 × 0.3 = 75 + 21 + 28,5, без бонуса
