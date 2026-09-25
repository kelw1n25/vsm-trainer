import os
from datetime import UTC, datetime, timedelta

from tests.test_engine import BEST_PATH, play

KEY = {"X-API-Key": os.environ["INTEGRATION_API_KEY"]}


def test_api_key_is_required(client, employee):
    assert client.get("/api/integration/employees/100001").json()["detail"]["code"] == "invalid_api_key"
    wrong = client.get("/api/integration/employees/100001", headers={"X-API-Key": "x" * 40})
    assert wrong.status_code == 401
    # JWT проводника не даёт доступа к интеграционному API
    assert client.get("/api/integration/employees/100001", headers={"Authorization": "Bearer x"}).status_code == 401


def test_competence_profile(client, auth):
    play(client, auth, *BEST_PATH)
    profile = client.get("/api/integration/employees/100001", headers=KEY).json()
    assert (profile["xp"], profile["level"], profile["runs_completed"], profile["success_rate"]) == (208, 1, 1, 1.0)
    assert profile["competences"]["service"] == 20
    assert sorted(profile["achievements"]) == ["diplomat", "first_trip", "flawless"]


def test_unknown_employee(client, employee):
    response = client.get("/api/integration/employees/999999", headers=KEY)
    assert response.status_code == 404
    assert client.get("/api/integration/employees/abc", headers=KEY).status_code == 422


def test_results_since(client, auth):
    before = (datetime.now(UTC) - timedelta(minutes=1)).isoformat()
    play(client, auth, *BEST_PATH)
    play(client, auth, "side_with_sergey", "raise_voice")

    results = client.get("/api/integration/results", params={"since": before}, headers=KEY).json()
    assert [r["outcome"] for r in results] == ["success", "failure"]
    assert results[0]["personnel_number"] == "100001"

    # Постраничная выгрузка: since = finished_at последней полученной записи
    rest = client.get("/api/integration/results", params={"since": results[0]["finished_at"]}, headers=KEY).json()
    assert [r["run_id"] for r in rest] == [results[1]["run_id"]]


def test_hr_upsert_creates_and_updates(client, db):
    body = {"full_name": "Синтетический Сотрудник", "brigade": "Бригада 7", "depot": "Депо Тверь"}
    first = client.put("/api/integration/employees/123456", json=body, headers=KEY).json()
    assert first == {"personnel_number": "123456", "created": True}

    moved = {**body, "brigade": "Бригада 8", "role": "instructor"}
    assert client.put("/api/integration/employees/123456", json=moved, headers=KEY).json()["created"] is False
    profile = client.get("/api/integration/employees/123456", headers=KEY).json()
    assert (profile["brigade"], profile["depot"], profile["role"]) == ("Бригада 8", "Депо Тверь", "instructor")

    # У сотрудника из HR нет пароля — войти по паролю нельзя
    login = client.post("/api/auth/login", json={"personnel_number": "123456", "password": "anything"})
    assert login.status_code == 401


def test_hr_upsert_validation(client, employee):
    response = client.put("/api/integration/employees/123456", json={"full_name": "A"}, headers=KEY)
    assert response.status_code == 422
    assert "brigade" in response.json()["detail"]["message"]


def test_billing_usage(client, auth, db):
    from sqlalchemy import func, select

    play(client, auth, *BEST_PATH)
    play(client, auth, "side_with_sergey", "raise_voice")
    month = db.scalar(select(func.to_char(func.timezone("Europe/Moscow", func.now()), "YYYY-MM")))
    usage = client.get("/api/integration/billing/usage", params={"month": month}, headers=KEY).json()
    assert (usage["active_employees"], usage["runs_completed"]) == (1, 2)
    assert usage["depots"][0]["xp_awarded"] > 0

    empty = client.get("/api/integration/billing/usage", params={"month": "2020-01"}, headers=KEY).json()
    assert (empty["runs_completed"], empty["depots"]) == (0, [])
    assert client.get("/api/integration/billing/usage", params={"month": "2026-13"}, headers=KEY).status_code == 422
