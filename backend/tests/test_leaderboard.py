import pytest

from app.auth.security import hash_password
from tests.conftest import login
from tests.test_engine import BEST_PATH, play


@pytest.fixture
def company(db):
    """Два депо: в первом две бригады. Сотрудник 100001 — в бригаде «А-1»."""
    from app.profiles.models import Brigade, Depot, Employee

    north, south = Depot(name="Депо Север"), Depot(name="Депо Юг")
    a1, a2, b1 = Brigade(name="А-1", depot=north), Brigade(name="А-2", depot=north), Brigade(name="Б-1", depot=south)
    people = [
        ("100001", a1, 0, "conductor"),
        ("100002", a1, 500, "conductor"),
        ("100003", a2, 900, "conductor"),
        ("100004", b1, 1500, "conductor"),
        ("900001", a1, 5000, "instructor"),
    ]
    for number, brigade, xp, role in people:
        db.add(Employee(
            full_name=f"Сотрудник {number}", personnel_number=number, password_hash=hash_password("secret"),
            role=role, brigade=brigade, xp=xp,
        ))
    db.commit()


def board(client, auth, scope, period):
    response = client.get(f"/api/leaderboard?scope={scope}&period={period}", headers=auth)
    assert response.status_code == 200, response.text
    return response.json()


def test_scopes_for_all_time(client, company):
    auth = login(client, "100001")
    assert [r["points"] for r in board(client, auth, "brigade", "all")["rows"]] == [500, 0]
    assert board(client, auth, "depot", "all")["participants"] == 3
    company_board = board(client, auth, "company", "all")
    assert [r["points"] for r in company_board["rows"]] == [1500, 900, 500, 0]
    # Инструктор в рейтинге не участвует
    assert all(r["full_name"] != "Сотрудник 900001" for r in company_board["rows"])


def test_current_user_is_marked(client, company):
    auth = login(client, "100001")
    rows = board(client, auth, "company", "all")["rows"]
    me = [r for r in rows if r["is_me"]]
    assert len(me) == 1 and me[0]["rank"] == 4


def test_week_counts_only_this_week_runs(client, company):
    auth = login(client, "100001")
    play(client, auth, *BEST_PATH)
    week = board(client, auth, "brigade", "week")["rows"]
    # На этой неделе XP заработал только 100001, у коллеги с 500 XP «за всё время» — 0
    assert (week[0]["full_name"], week[0]["points"], week[0]["rank"]) == ("Сотрудник 100001", 208, 1)
    assert week[1]["points"] == 0


def test_equal_points_share_rank(client, company):
    auth = login(client, "100001")
    rows = board(client, auth, "company", "week")["rows"]
    assert {r["rank"] for r in rows} == {1}


def test_invalid_scope(client, company):
    auth = login(client, "100001")
    response = client.get("/api/leaderboard?scope=galaxy", headers=auth)
    assert response.status_code == 422
