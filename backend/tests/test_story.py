from tests.test_engine import BEST_PATH, play


def story_map(client, auth) -> dict:
    response = client.get("/api/scenarios/business-seat-conflict/story-map", headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def test_new_player_sees_only_the_start(client, auth):
    data = story_map(client, auth)
    assert (data["playthroughs"], data["choices_explored"]) == (0, 0)
    assert [node["id"] for node in data["nodes"]] == ["start"]
    # Содержание неисследованных веток не отдаётся даже в ответе API
    assert all(choice["text"] is None and choice["next"] is None for choice in data["nodes"][0]["choices"])
    assert all(ending["ending"] is None and not ending["reached"] for ending in data["endings"])


def test_explored_branches_and_endings_are_remembered(client, auth):
    play(client, auth, *BEST_PATH)
    play(client, auth, "side_with_sergey", "raise_voice")
    data = story_map(client, auth)

    assert data["playthroughs"] == 2
    assert data["choices_explored"] == 5
    start = next(node for node in data["nodes"] if node["id"] == "start")
    explored = {choice["id"]: choice["explored"] for choice in start["choices"]}
    assert explored == {"check_both": True, "side_with_sergey": True, "tell_wait": False}
    reached = {ending["id"]: ending["ending"] for ending in data["endings"] if ending["reached"]}
    assert reached == {"end_success": "Оба пассажира довольны", "end_conflict": "Скандал в бизнес-классе"}
    # Узел, в который игрок не заходил, на карте не появляется
    assert "end_complaint" not in {node["id"] for node in data["nodes"]}


def test_story_map_of_unknown_scenario(client, auth):
    assert client.get("/api/scenarios/nope/story-map", headers=auth).status_code == 404


def test_handbook(client, auth):
    data = client.get("/api/handbook", headers=auth).json()
    assert len(data["situations"]) == 51
    assert [step["title"] for step in data["role_model"]["steps"]] == [
        "Признать ситуацию", "Обозначить правило", "Предложить решение", "Заверить",
    ]
    assert [c["title"] for c in data["service_classes"]] == ["Первый", "Бизнес", "Комфорт", "Стандарт"]
    assert client.get("/api/handbook").status_code == 401
