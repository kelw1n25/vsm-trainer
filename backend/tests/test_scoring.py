import copy

import pytest
import yaml

from app.scoring.service import calculate_xp, clamp_scale, fast_answer_bonus
from tests.test_engine import choose, play, start
from tests.test_timer import rewind
from tests.test_validator import MINIMAL


def test_clamp_scale():
    assert clamp_scale(-5) == 0
    assert clamp_scale(130) == 100
    assert clamp_scale(42) == 42


def test_calculate_xp():
    # success: 100 × 1.5 (сложность 2) + 95 × 0.3 + 85 × 0.3
    assert calculate_xp("success", 2, 95, 85) == 204
    assert calculate_xp("failure", 1, 0, 50) == 25


def test_fast_answer_bonus():
    assert fast_answer_bonus(30, 5) == {"stress_resistance": 3}
    assert fast_answer_bonus(30, 15) == {}
    assert fast_answer_bonus(None, 1) == {}


def test_finish_awards_xp_and_competences(client, auth, db, employee):
    state = play(client, auth, "ask_tickets", "explain_calmly", "reissue_ticket")
    assert state["final"]["xp_earned"] == 204
    db.refresh(employee)
    assert employee.xp == 204
    # communication 10 + 10, safety_rules 5 + 10, service 10 + 15, плюс бонусы за быстрые ответы
    assert employee.competence_points == {
        "communication": 20, "safety_rules": 15, "service": 25, "stress_resistance": 6,
    }
    assert employee.last_activity_at is not None


def test_slow_answer_gets_no_speed_bonus(client, auth, db):
    state = start(client, auth)
    rewind(db, state["id"], 10)  # половина 20-секундного таймера
    step = choose(client, auth, state, "ask_tickets").json()["last_steps"][0]
    assert step["competences"] == {"communication": 10, "safety_rules": 5}


def test_negative_points_do_not_make_profile_negative(client, auth, db, employee):
    play(client, auth, "demand_leave", "ask_neighbours")
    db.refresh(employee)
    assert all(value >= 0 for value in employee.competence_points.values())


@pytest.fixture
def risky_scenario(db, tmp_path):
    from app.scenarios.loader import load_scenarios

    data = copy.deepcopy(MINIMAL)
    data["id"] = "risky"
    data["nodes"][0]["choices"][1]["effects"] = {"safety": -100, "loyalty": 80}
    (tmp_path / "risky.yaml").write_text(yaml.safe_dump(data, allow_unicode=True), encoding="utf-8")
    load_scenarios(db, tmp_path)
    return "risky"


def test_depleted_scale_fails_run_early(client, auth, risky_scenario):
    state = client.post("/api/runs", json={"scenario_id": risky_scenario}, headers=auth).json()
    state = choose(client, auth, state, "bad").json()
    assert state["status"] == "failure"
    assert state["final"]["reason"] == "safety_depleted"
    assert (state["loyalty"], state["safety"]) == (100, 0)  # лояльность упёрлась в 100
    assert state["last_steps"][0]["loyalty_delta"] == 50
    # Повторный запрос показывает ту же причину: она сохранена в БД
    again = client.get(f"/api/runs/{state['id']}", headers=auth).json()
    assert again["final"]["reason"] == "safety_depleted"
    assert "безопасност" in again["final"]["text"]
