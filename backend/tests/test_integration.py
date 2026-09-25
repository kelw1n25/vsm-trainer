import os
from datetime import UTC, datetime, timedelta

from tests.test_engine import play

KEY = {"X-API-Key": os.environ["INTEGRATION_API_KEY"]}


def test_api_key_is_required(client, employee):
    assert client.get("/api/integration/employees/100001").json()["detail"]["code"] == "invalid_api_key"
    wrong = client.get("/api/integration/employees/100001", headers={"X-API-Key": "x" * 40})
    assert wrong.status_code == 401
    # JWT проводника не даёт доступа к интеграционному API
    assert client.get("/api/integration/employees/100001", headers={"Authorization": "Bearer x"}).status_code == 401


def test_competence_profile(client, auth):
    play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    profile = client.get("/api/integration/employees/100001", headers=KEY).json()
    assert (profile["xp"], profile["level"], profile["runs_completed"], profile["success_rate"]) == (204, 1, 1, 1.0)
    assert profile["competences"]["service"] == 25
    assert sorted(profile["achievements"]) == ["diplomat", "flawless"]


def test_unknown_employee(client, employee):
    response = client.get("/api/integration/employees/999999", headers=KEY)
    assert response.status_code == 404
    assert client.get("/api/integration/employees/abc", headers=KEY).status_code == 422


def test_results_since(client, auth):
    before = (datetime.now(UTC) - timedelta(minutes=1)).isoformat()
    play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    play(client, auth, "demand_leave", "ask_neighbours")

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
