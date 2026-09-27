from app.auth.security import hash_password
from tests.conftest import create_employee, login
from tests.test_engine import PARTIAL_PATH, choose, play, start
from tests.test_timer import at_timed_node, rewind


def analytics(client, auth) -> dict:
    response = client.get("/api/analytics/me", headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def test_new_employee(client, auth):
    data = analytics(client, auth)
    assert (data["total_runs"], data["mistakes"], data["weaknesses"]) == (0, [], [])
    assert data["recommendation"]["reason"].startswith("Начните")


def test_frequent_timeouts_are_detected(client, auth, db):
    for _ in range(3):
        state = at_timed_node(client, auth)
        rewind(db, state["id"], 22)  # таймер спора пассажиров истёк
        state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
        choose(client, auth, state, "promise_now")

    data = analytics(client, auth)
    conflict = data["categories"][0]
    assert (conflict["runs"], conflict["timed_steps"], conflict["timeout_rate"]) == (3, 3, 1.0)
    assert any(m.startswith("Часто истекает таймер в конфликтных сценариях") for m in data["mistakes"])


def test_rare_best_choices_and_progress(client, auth):
    play(client, auth, "side_with_sergey", "raise_voice")
    play(client, auth, "tell_wait", "raise_voice")
    data = analytics(client, auth)
    assert data["categories"][0]["best_choice_rate"] == 0.0
    assert any("Лучшее решение в конфликтных сценариях выбирается редко" in m for m in data["mistakes"])
    week = data["progress"][-1]
    assert (week["runs"], week["successes"]) == (2, 0)
    assert week["xp"] > 0


def test_recommendation_targets_weakest_competence(client, auth):
    play(client, auth, *PARTIAL_PATH)
    data = analytics(client, auth)
    assert data["weaknesses"][0] == "Первая помощь и медицинские ситуации"
    # Больше всего очков первой помощи даёт медицинский сценарий
    assert data["recommendation"]["scenario_id"] == "passenger-unwell"
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


def test_completion_rate_and_decision_time(client, auth):
    play(client, auth, *PARTIAL_PATH)
    start(client, auth, "passenger-unwell")  # начат и брошен
    data = analytics(client, auth)
    assert (data["total_runs"], data["unfinished_runs"], data["completion_rate"]) == (1, 1, 0.5)
    assert data["avg_decision_seconds"] is not None and data["avg_decision_seconds"] >= 0


def test_client_events_are_recorded(client, auth, db):
    from sqlalchemy import select

    from app.analytics.models import Event

    finished = play(client, auth, *PARTIAL_PATH)
    for event in (
        {"type": "debrief_opened", "run_id": finished["id"], "platform": "android"},
        {"type": "run_exited", "run_id": start(client, auth, "passenger-unwell")["id"], "platform": "android"},
    ):
        assert client.post("/api/analytics/events", json=event, headers=auth).status_code == 202

    notification = client.get("/api/notifications", headers=auth).json()["items"][0]
    opened = {"type": "notification_opened", "notification_id": notification["id"], "platform": "web"}
    assert client.post("/api/analytics/events", json=opened, headers=auth).status_code == 202

    recorded = {e.type: e.payload for e in db.scalars(select(Event)).all()}
    assert recorded["debrief_opened"] == {"platform": "android"}
    assert recorded["notification_opened"] == {"platform": "web", "notification_type": notification["type"]}


def test_client_events_reject_foreign_and_unknown(client, auth, db):
    create_employee(db, "100002")
    foreign = play(client, login(client, "100002"), *PARTIAL_PATH)["id"]
    event = {"type": "debrief_opened", "run_id": foreign, "platform": "android"}
    assert client.post("/api/analytics/events", json=event, headers=auth).status_code == 404
    unknown = {"type": "xp_granted", "platform": "android"}
    assert client.post("/api/analytics/events", json=unknown, headers=auth).status_code == 422
