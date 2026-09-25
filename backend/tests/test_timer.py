import uuid
from datetime import timedelta

from sqlalchemy import select, update

from tests.test_engine import choose, play, reveal, start


def rewind(db, run_id: str, seconds: float) -> None:
    """Сдвигает в прошлое вход в узел и показ вариантов — как будто прошло seconds секунд."""
    from app.engine.models import ScenarioRun

    db.execute(
        update(ScenarioRun)
        .where(ScenarioRun.id == uuid.UUID(run_id))
        .values(
            node_entered_at=ScenarioRun.node_entered_at - timedelta(seconds=seconds),
            choices_shown_at=ScenarioRun.choices_shown_at - timedelta(seconds=seconds),
        )
    )
    db.commit()


def at_timed_node(client, auth) -> dict:
    """Узел «спор пассажиров»: таймер 20 с, по истечении — приходит начальник поезда, лояльность −15."""
    state = play(client, auth, "side_with_sergey")
    assert state["node"]["id"] == "argument"
    return reveal(client, auth, state).json()


def test_timer_starts_only_after_reveal(client, auth, db):
    state = play(client, auth, "side_with_sergey")
    assert state["node"]["deadline_at"] is None
    rewind(db, state["id"], 100)  # сцену читали долго — это не в счёт

    state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    assert state["node"]["id"] == "argument"
    assert reveal(client, auth, state).json()["node"]["deadline_at"] is not None


def test_expired_timer_moves_to_timeout_node(client, auth, db):
    state = at_timed_node(client, auth)
    rewind(db, state["id"], 22)  # таймер 20 с + льгота 1 с

    state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    assert state["node"]["id"] == "chief_resolves"
    assert state["loyalty"] == 30  # 60 − 15 за резкое решение − 15 за истечение таймера
    [step] = state["last_steps"]
    assert step["kind"] == "timeout"
    assert step["reaction"][0]["kind"] == "narration"
    # Следующий узел ждёт, пока проводник дочитает сцену
    assert state["node"]["choices_shown"] is False


def test_answer_after_deadline_is_rejected(client, auth, db):
    state = at_timed_node(client, auth)
    rewind(db, state["id"], 22)

    response = choose(client, auth, state, "separate", reveal_first=False)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "time_expired"
    # Таймаут сохранён, хотя ответ отклонён
    assert client.get(f"/api/runs/{state['id']}", headers=auth).json()["node"]["id"] == "chief_resolves"


def test_answer_within_grace_is_accepted(client, auth, db):
    state = at_timed_node(client, auth)
    rewind(db, state["id"], 20.5)
    assert choose(client, auth, state, "separate", reveal_first=False).status_code == 200


def test_client_cannot_extend_timer_by_refreshing(client, auth, db):
    state = at_timed_node(client, auth)
    deadline = state["node"]["deadline_at"]
    assert client.get(f"/api/runs/{state['id']}", headers=auth).json()["node"]["deadline_at"] == deadline
    assert reveal(client, auth, state).json()["node"]["deadline_at"] == deadline
    assert start(client, auth)["node"]["deadline_at"] == deadline


def test_resume_after_deadline_applies_timeout(client, auth, db):
    state = at_timed_node(client, auth)
    rewind(db, state["id"], 22)
    assert start(client, auth)["node"]["id"] == "chief_resolves"


def test_timeout_is_logged(client, auth, db):
    from app.analytics.models import Event

    state = at_timed_node(client, auth)
    rewind(db, state["id"], 22)
    client.get(f"/api/runs/{state['id']}", headers=auth)
    event = db.scalar(select(Event).where(Event.type == "timeout"))
    assert event.payload["node_id"] == "argument"
    assert event.payload["loyalty_delta"] == -15


def test_reveal_on_stale_node_is_rejected(client, auth):
    state = start(client, auth)
    choose(client, auth, state, "check_both")
    response = reveal(client, auth, state)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "stale_node"
