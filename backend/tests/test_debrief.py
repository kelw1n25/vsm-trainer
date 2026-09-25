from tests.test_engine import choose, play, start
from tests.test_timer import at_timed_node, rewind


def test_debrief_requires_finished_run(client, auth):
    state = start(client, auth)
    response = client.get(f"/api/runs/{state['id']}/debrief", headers=auth)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "run_in_progress"


def test_debrief_shows_each_decision_and_best_option(client, auth):
    state = play(client, auth, "side_with_sergey", "separate", "call_chief", "promise_now")
    debrief = client.get(f"/api/runs/{state['id']}/debrief", headers=auth).json()

    assert (debrief["outcome"], debrief["ending"]) == ("partial", "Места распределены, но осадок остался")
    assert (debrief["decisions"], debrief["best_decisions"], debrief["timeouts"]) == (4, 2, 0)
    first, second, _, fourth = debrief["steps"]
    assert first["was_best"] is False
    assert first["chosen_text"].startswith("Попросить женщину освободить")
    assert first["best_text"].startswith("«Давайте решим вопрос спокойно")
    assert "без проверки обоих билетов" in first["explanation"]
    assert first["loyalty_delta"] == -15 and first["loyalty_after"] == 45
    assert second["was_best"] is True
    assert fourth["best_text"].startswith("«Сейчас я уточню наличие")


def test_debrief_links_handbook_situations(client, auth):
    state = play(client, auth, "side_with_sergey", "raise_voice")
    debrief = client.get(f"/api/runs/{state['id']}/debrief", headers=auth).json()
    assert [s["number"] for s in debrief["situations"]] == [33, 13, 9, 8, 23]
    assert debrief["situations"][0]["title"] == "Два пассажира на одно место"
    assert debrief["situations"][0]["phrases"]


def test_debrief_includes_timeouts(client, auth, db):
    state = at_timed_node(client, auth)
    rewind(db, state["id"], 22)
    state = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    choose(client, auth, state, "proper_upgrade")

    debrief = client.get(f"/api/runs/{state['id']}/debrief", headers=auth).json()
    assert [s["kind"] for s in debrief["steps"]] == ["choice", "timeout", "choice"]
    assert debrief["steps"][1]["chosen_text"] is None
    assert debrief["steps"][1]["best_text"].startswith("Развести коммуникацию")
    assert debrief["average_reaction_seconds"] is not None
