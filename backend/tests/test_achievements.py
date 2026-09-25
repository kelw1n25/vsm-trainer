import copy

import pytest
import yaml

from app.achievements.service import level_for
from tests.test_engine import choose, play
from tests.test_validator import MINIMAL


def test_level_for():
    assert (level_for(0).level, level_for(0).next_level_xp) == (1, 300)
    assert level_for(299).level == 1
    assert level_for(300).title == "Проводник"
    assert level_for(10_000).next_level_xp is None


def test_diplomat_and_flawless_for_clean_conflict(client, auth):
    # Лучший путь: лояльность 95 > 90, таймеров не истекало, шкалы не опускались ниже 50
    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    assert {a["code"] for a in state["new_achievements"]} == {"diplomat", "flawless"}


def test_achievement_is_given_once(client, auth):
    play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    assert state["new_achievements"] == []


def test_partial_result_is_not_flawless(client, auth):
    state = play(client, auth, "ask_tickets", "explain_calmly", "leave_as_is")
    assert "flawless" not in {a["code"] for a in state["new_achievements"]}


@pytest.fixture
def quick_scenario(db, tmp_path):
    """Сценарий из одного решения с таймером — удобно набирать серию быстрых ответов."""
    from app.scenarios.loader import load_scenarios

    data = copy.deepcopy(MINIMAL)
    data["id"] = "quick"
    (tmp_path / "quick.yaml").write_text(yaml.safe_dump(data, allow_unicode=True), encoding="utf-8")
    load_scenarios(db, tmp_path)
    return "quick"


def test_cool_head_after_five_fast_decisions(client, auth, quick_scenario):
    earned = []
    for _ in range(5):
        state = client.post("/api/runs", json={"scenario_id": quick_scenario}, headers=auth).json()
        earned.append([a["code"] for a in choose(client, auth, state, "good").json()["new_achievements"]])
    assert "cool_head" not in sum(earned[:4], [])
    assert "cool_head" in earned[4]


def test_level_up_is_reported(client, auth, db, employee):
    employee.xp = 290
    db.commit()
    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    assert state["level_up"]["title"] == "Проводник"


def test_profile(client, auth):
    play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    profile = client.get("/api/profile", headers=auth).json()
    assert profile["level"] == {"level": 1, "title": "Стажёр", "xp": 204, "level_xp": 0, "next_level_xp": 300}
    assert profile["competence_points"]["first_aid"] == 0
    earned = {a["code"] for a in profile["achievements"] if a["earned_at"]}
    assert earned == {"diplomat", "flawless"}
    assert len(profile["achievements"]) == 4
    assert profile["history"][0]["outcome"] == "success"
    assert profile["brigade"] == "Бригада 1"
