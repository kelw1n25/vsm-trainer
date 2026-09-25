from tests.test_engine import play, start
from tests.test_timer import rewind


def test_debrief_requires_finished_run(client, auth):
    state = start(client, auth)
    response = client.get(f"/api/runs/{state['id']}/debrief", headers=auth)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "run_in_progress"


def test_debrief_shows_each_decision_and_best_option(client, auth):
    state = play(client, auth, "demand_leave", "separate_and_call", "pass_to_chief")
    debrief = client.get(f"/api/runs/{state['id']}/debrief", headers=auth).json()

    assert debrief["outcome"] == "partial"
    assert (debrief["decisions"], debrief["best_decisions"], debrief["timeouts"]) == (3, 1, 0)
    first, second, third = debrief["steps"]
    assert first["was_best"] is False
    assert first["chosen_text"].startswith("Сразу потребовать")
    assert first["best_text"].startswith("Подойти, представиться")
    assert "проверки фактов" in first["explanation"]
    assert first["loyalty_delta"] == -15 and first["loyalty_after"] == 45
    assert second["was_best"] is True
    assert third["best_text"].startswith("Предложить начальнику вместе")


def test_debrief_includes_timeouts(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 20 + 15 + 2)
    client.get(f"/api/runs/{state['id']}", headers=auth)

    debrief = client.get(f"/api/runs/{state['id']}/debrief", headers=auth).json()
    assert [s["kind"] for s in debrief["steps"]] == ["timeout", "timeout"]
    assert debrief["steps"][0]["chosen_text"] is None
    assert debrief["steps"][0]["best_text"].startswith("Подойти, представиться")
    assert debrief["average_reaction_seconds"] is None
