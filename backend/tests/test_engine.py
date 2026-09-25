from sqlalchemy import select

from tests.conftest import create_employee, login

SCENARIO = "business-seat-conflict"
BEST_PATH = ("check_both", "call_chief", "proper_upgrade")
# Обещание без проверки мест — частичный успех: лояльность 70, безопасность 95
PARTIAL_PATH = ("check_both", "call_chief", "promise_now")


def start(client, auth, scenario: str = SCENARIO) -> dict:
    response = client.post("/api/runs", json={"scenario_id": scenario}, headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def reveal(client, auth, state: dict):
    """Проводник дочитал сцену — сервер показывает варианты и запускает таймер."""
    return client.post(f"/api/runs/{state['id']}/reveal", json={"node_id": state["node"]["id"]}, headers=auth)


def choose(client, auth, state: dict, choice_id: str, reveal_first: bool = True):
    if reveal_first:
        reveal(client, auth, state)
    return client.post(
        f"/api/runs/{state['id']}/choices",
        json={"node_id": state["node"]["id"], "choice_id": choice_id},
        headers=auth,
    )


def play(client, auth, *choice_ids: str, scenario: str = SCENARIO) -> dict:
    state = start(client, auth, scenario)
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
    assert len(catalog) == 14
    assert {s["category"] for s in catalog} == {"conflict", "medical", "service", "safety"}
    assert all(s["description"] and s["situations"] and s["endings_total"] >= 3 for s in catalog)
    assert {s["service_class"] for s in catalog} == {"Первый", "Бизнес", "Комфорт", "Стандарт"}


def test_start_shows_scene_without_choices(client, auth):
    state = start(client, auth)
    assert state["status"] == "in_progress"
    assert (state["loyalty"], state["safety"]) == (60, 90)
    node = state["node"]
    assert node["id"] == "start"
    assert node["scene"]["background"] == "business"
    assert [c["id"] for c in node["scene"]["characters"]] == ["player", "sergey", "lyudmila"]
    assert [line["kind"] for line in node["dialogue"]] == ["narration", "speech", "speech", "thought"]
    assert node["dialogue"][1]["name"] == "Сергей Андреевич"
    # Пока сцена не дочитана, вариантов нет и таймер не идёт
    assert (node["choices_shown"], node["choices"], node["deadline_at"]) == (False, [], None)
    assert {c["id"] for c in state["characters"]} == {"sergey", "lyudmila", "chief"}
    assert (state["category"], state["difficulty"], state["steps_taken"], state["steps_left"]) == ("conflict", 2, 0, 2)


def test_reveal_shows_choices(client, auth):
    state = reveal(client, auth, start(client, auth)).json()
    assert state["node"]["choices_shown"] is True
    assert [c["id"] for c in state["node"]["choices"]] == ["check_both", "side_with_sergey", "tell_wait"]


def test_answer_before_reveal_is_rejected(client, auth):
    state = start(client, auth)
    response = choose(client, auth, state, "check_both", reveal_first=False)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "choices_not_shown"


def test_repeated_start_resumes_run(client, auth):
    assert start(client, auth)["id"] == start(client, auth)["id"]


def test_best_path_reaches_success(client, auth):
    state = play(client, auth, *BEST_PATH)
    assert state["status"] == "success"
    assert state["node"] is None
    final = state["final"]
    assert (final["outcome"], final["ending"]) == ("success", "Оба пассажира довольны")
    assert (state["loyalty"], state["safety"]) == (95, 100)
    assert state["last_steps"][0]["loyalty_delta"] == 15
    assert [line["speaker"] for line in state["last_steps"][0]["reaction"]] == ["lyudmila", "player"]
    assert state["steps_taken"] == 3


def test_choice_reaction_is_returned(client, auth):
    state = play(client, auth, "check_both")
    assert [line["text"] for line in state["last_steps"][0]["reaction"]] == ["Пожалуйста, вот.", "И мой, в приложении."]
    assert state["node"]["id"] == "tickets_checked"


def test_trade_off_between_scales(client, auth):
    # «Прослежу, сколько нужно» радует отца, но проводник берёт чужую ответственность — безопасность падает
    state = play(client, auth, "full_responsibility", scenario="lost-child")
    assert (state["loyalty"], state["safety"]) == (75, 60)


def test_hidden_branch_depends_on_hidden_stat(client, auth, db):
    trusted = reveal(client, auth, play(client, auth, "check_both")).json()
    assert "free_seats" in [c["id"] for c in trusted["node"]["choices"]]

    other = login(client, create_employee(db, "100002").personnel_number)
    distrusted = reveal(client, other, play(client, other, "side_with_sergey", "separate")).json()
    assert distrusted["node"]["id"] == "tickets_checked"
    assert "free_seats" not in [c["id"] for c in distrusted["node"]["choices"]]


def test_hidden_stats_are_not_exposed(client, auth):
    state = play(client, auth, "check_both")
    assert "stats" not in state and "trust" not in str(state)


def test_hidden_choice_cannot_be_submitted(client, auth):
    state = play(client, auth, "side_with_sergey", "separate")
    response = choose(client, auth, state, "free_seats")
    assert response.status_code == 422
    assert response.json()["detail"]["code"] == "unknown_choice"


def test_repeated_answer_is_rejected(client, auth):
    state = start(client, auth)
    assert choose(client, auth, state, "check_both").status_code == 200
    response = choose(client, auth, state, "check_both", reveal_first=False)
    assert response.status_code == 409
    assert response.json()["detail"]["code"] == "stale_node"


def test_answer_after_finish_is_rejected(client, auth):
    state = play(client, auth, "side_with_sergey", "raise_voice")
    assert state["status"] == "failure"
    assert state["final"]["ending"] == "Скандал в бизнес-классе"
    response = client.post(
        f"/api/runs/{state['id']}/choices", json={"node_id": "end_conflict", "choice_id": "x"}, headers=auth
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

    state = play(client, auth, *BEST_PATH)
    events = db.scalars(select(Event).where(Event.run_id == state["id"]).order_by(Event.id)).all()
    assert [e.type for e in events] == [
        "run_started",
        "choices_shown", "choice_made", "choices_shown", "choice_made", "choices_shown", "choice_made",
        "run_finished", "achievement_earned", "achievement_earned", "achievement_earned",
    ]
    assert events[2].payload["choice_id"] == "check_both"
    assert events[2].payload["elapsed_seconds"] >= 0
