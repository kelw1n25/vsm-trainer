from sqlalchemy import select

from tests.conftest import create_employee, login

SCENARIO = "business-seat-conflict"


def start(client, auth) -> dict:
    response = client.post("/api/runs", json={"scenario_id": SCENARIO}, headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def choose(client, auth, state: dict, choice_id: str):
    return client.post(
        f"/api/runs/{state['id']}/choices",
        json={"node_id": state["node"]["id"], "choice_id": choice_id},
        headers=auth,
    )


def play(client, auth, *choice_ids: str) -> dict:
    state = start(client, auth)
    for choice_id in choice_ids:
        response = choose(client, auth, state, choice_id)
        assert response.status_code == 200, response.text
        state = response.json()
    return state


def test_login_errors(client, employee):
    response = client.post("/api/auth/login", json={"personnel_number": "100001", "password": "wrong"})
    assert response.status_code == 401
    assert response.json()["detail"]["code"] == "invalid_credentials"
    assert client.get("/api/scenarios").json()["detail"]["code"] == "not_authenticated"


def test_catalog(client, auth):
    catalog = client.get("/api/scenarios", headers=auth).json()
    assert len(catalog) == 9
    assert {s["category"] for s in catalog} == {"conflict", "medical", "service", "safety"}
    assert all(s["demo"] and s["description"] for s in catalog)


def test_start_shows_first_node(client, auth):
    state = start(client, auth)
    assert state["status"] == "in_progress"
    assert (state["loyalty"], state["safety"]) == (60, 80)
    assert state["node"]["id"] == "start"
    assert [c["id"] for c in state["node"]["choices"]] == ["ask_tickets", "demand_leave", "offer_temp_seat"]
    assert state["node"]["timer_seconds"] == 20
    assert state["node"]["deadline_at"] is not None
    assert (state["category"], state["difficulty"], state["steps_taken"]) == ("conflict", 2, 0)


def test_repeated_start_resumes_run(client, auth):
    assert start(client, auth)["id"] == start(client, auth)["id"]


def test_best_path_reaches_success(client, auth):
    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    assert state["status"] == "success"
    assert state["node"] is None
    assert state["final"]["outcome"] == "success"
    assert (state["loyalty"], state["safety"]) == (95, 85)
    assert state["last_steps"][0]["loyalty_delta"] == 15
    assert state["steps_taken"] == 3


def test_trade_off_between_scales(client, auth):
    # «Оставить без оформления» поднимает лояльность, но роняет безопасность
    state = play(client, auth, "ask_tickets", "explain_calmly", "leave_as_is")
    assert state["status"] == "partial"
    assert (state["loyalty"], state["safety"]) == (85, 65)


def test_conditional_choice_depends_on_flag(client, auth, db):
    without_flag = play(client, auth, "demand_leave", "separate_and_call")
    assert without_flag["node"]["id"] == "chief_arrives"
    assert "report_with_facts" not in [c["id"] for c in without_flag["node"]["choices"]]

    other = login(client, create_employee(db, "100002").personnel_number)
    with_flag = play(client, other, "ask_tickets", "threaten_fine", "separate_and_call")
    assert "report_with_facts" in [c["id"] for c in with_flag["node"]["choices"]]


def test_hidden_choice_cannot_be_submitted(client, auth):
    state = play(client, auth, "demand_leave", "separate_and_call")
    response = choose(client, auth, state, "report_with_facts")
    assert response.status_code == 422
    assert response.json()["detail"]["code"] == "unknown_choice"


def test_repeated_answer_is_rejected(client, auth):
    state = start(client, auth)
    assert choose(client, auth, state, "ask_tickets").status_code == 200
    response = choose(client, auth, state, "ask_tickets")
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "stale_node"


def test_answer_after_finish_is_rejected(client, auth):
    state = play(client, auth, "demand_leave", "ask_neighbours")
    assert state["status"] == "failure"
    response = client.post(
        f"/api/runs/{state['id']}/choices", json={"node_id": "final_fail", "choice_id": "x"}, headers=auth
    )
    assert response.json()["detail"]["code"] == "run_finished"


def test_other_employee_cannot_see_run(client, auth, db):
    state = start(client, auth)
    other = login(client, create_employee(db, "100002").personnel_number)
    assert client.get(f"/api/runs/{state['id']}", headers=other).status_code == 404


def test_unknown_scenario(client, auth):
    response = client.post("/api/runs", json={"scenario_id": "nope"}, headers=auth)
    assert response.status_code == 404
    assert response.json()["detail"]["code"] == "scenario_not_found"


def test_invalid_request_body(client, auth):
    response = client.post("/api/runs", json={}, headers=auth)
    assert response.status_code == 422
    assert "scenario_id" in response.json()["detail"]["message"]


def test_actions_are_logged(client, auth, db):
    from app.analytics.models import Event

    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    events = db.scalars(select(Event).where(Event.run_id == state["id"]).order_by(Event.id)).all()
    assert [e.type for e in events] == [
        "run_started", "choice_made", "choice_made", "choice_made", "run_finished",
        "achievement_earned", "achievement_earned", "achievement_earned",
    ]
    assert events[1].payload["choice_id"] == "ask_tickets"
    assert events[1].payload["elapsed_seconds"] >= 0
