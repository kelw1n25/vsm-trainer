import uuid
from datetime import timedelta

from sqlalchemy import select, update

from tests.test_engine import choose, start


def rewind(db, run_id: str, seconds: float) -> None:
    """Сдвигает момент входа в текущий узел в прошлое — как будто прошло seconds секунд."""
    from app.engine.models import ScenarioRun

    db.execute(
        update(ScenarioRun)
        .where(ScenarioRun.id == uuid.UUID(run_id))
        .values(node_entered_at=ScenarioRun.node_entered_at - timedelta(seconds=seconds))
    )
    db.commit()


def test_expired_timer_moves_to_timeout_node(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 22)  # таймер 20 с + льгота 1 с

    state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    assert state["node"]["id"] == "escalation"
    assert state["loyalty"] == 45  # 60 − 15 за истечение таймера
    assert [s["kind"] for s in state["last_steps"]] == ["timeout"]


def test_answer_after_deadline_is_rejected(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 22)

    response = choose(client, auth, state, "ask_tickets")
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "time_expired"
    # Таймаут сохранён, хотя ответ отклонён
    assert client.get(f"/api/runs/{state['id']}", headers=auth).json()["node"]["id"] == "escalation"


def test_answer_within_grace_is_accepted(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 20.5)
    assert choose(client, auth, state, "ask_tickets").status_code == 200


def test_missed_timers_cascade_to_final(client, auth, db):
    state = start(client, auth)
    # start (20 с) → escalation (15 с) → final_fail
    rewind(db, state["id"], 20 + 15 + 2)

    state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    assert state["status"] == "failure"
    assert [s["kind"] for s in state["last_steps"]] == ["timeout", "timeout"]
    assert (state["loyalty"], state["safety"]) == (30, 60)


def test_client_cannot_extend_timer_by_refreshing(client, auth, db):
    state = start(client, auth)
    deadline = state["node"]["deadline_at"]
    assert client.get(f"/api/runs/{state['id']}", headers=auth).json()["node"]["deadline_at"] == deadline
    assert client.post("/api/runs", json={"scenario_id": state["scenario_id"]}, headers=auth).json()["node"][
        "deadline_at"
    ] == deadline


def test_resume_after_deadline_applies_timeout(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 22)
    assert start(client, auth)["node"]["id"] == "escalation"


def test_timeout_is_logged(client, auth, db):
    from app.analytics.models import Event

    state = start(client, auth)
    rewind(db, state["id"], 22)
    client.get(f"/api/runs/{state['id']}", headers=auth)
    event = db.scalar(select(Event).where(Event.type == "timeout"))
    assert event.payload["node_id"] == "start"
    assert event.payload["loyalty_delta"] == -15
