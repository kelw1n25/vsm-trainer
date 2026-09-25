from app.auth.security import hash_password
from tests.conftest import create_employee, login
from tests.test_engine import play, start
from tests.test_timer import rewind


def analytics(client, auth) -> dict:
    response = client.get("/api/analytics/me", headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def test_new_employee(client, auth):
    data = analytics(client, auth)
    assert (data["total_runs"], data["mistakes"], data["weaknesses"]) == (0, [], [])
    assert data["recommendation"]["reason"].startswith("Начните")


def test_frequent_timeouts_are_detected(client, auth, db):
    for _ in range(2):
        state = start(client, auth)
        rewind(db, state["id"], 20 + 15 + 2)  # оба таймера истекли — провал
        client.get(f"/api/runs/{state['id']}", headers=auth)

    data = analytics(client, auth)
    conflict = data["categories"][0]
    assert (conflict["runs"], conflict["timed_steps"], conflict["timeout_rate"]) == (2, 4, 1.0)
    assert any(m.startswith("Часто истекает таймер в конфликтных сценариях") for m in data["mistakes"])


def test_rare_best_choices_and_progress(client, auth):
    play(client, auth, "demand_leave", "ask_neighbours")
    play(client, auth, "offer_temp_seat", "offer_meal")
    data = analytics(client, auth)
    assert data["categories"][0]["best_choice_rate"] == 0.0
    assert any("Лучшее решение в конфликтных сценариях выбирается редко" in m for m in data["mistakes"])
    week = data["progress"][-1]
    assert (week["runs"], week["successes"]) == (2, 0)
    assert week["xp"] > 0


def test_recommendation_targets_weakest_competence(client, auth):
    play(client, auth, "ask_tickets", "explain_calmly", "leave_as_is")
    data = analytics(client, auth)
    assert data["weaknesses"][0] == "Первая помощь и медицинские ситуации"
    assert data["recommendation"]["scenario_id"] == "business-seat-conflict"
    assert "Первая помощь" in data["recommendation"]["reason"]


def test_instructor_access(client, db, employee):
    from app.profiles.models import Brigade, Employee

    conductor = login(client, employee.personnel_number)
    assert client.get("/api/analytics/team", headers=conductor).status_code == 403

    # Инструктор того же депо видит проводника, инструктор другого депо — нет
    db.add(Employee(
        full_name="Инструктор", personnel_number="900001", password_hash=hash_password("secret"),
        role="instructor", brigade=Brigade(name="Инструкторы", depot=employee.brigade.depot),
    ))
    db.commit()
    instructor = login(client, "900001")
    team = client.get("/api/analytics/team", headers=instructor).json()
    assert [m["employee_id"] for m in team] == [employee.id]
    assert client.get(f"/api/analytics/employees/{employee.id}", headers=instructor).status_code == 200

    create_employee(db, "900002", role="instructor")  # создаётся в своём отдельном депо
    stranger = login(client, "900002")
    assert client.get(f"/api/analytics/employees/{employee.id}", headers=stranger).status_code == 404
