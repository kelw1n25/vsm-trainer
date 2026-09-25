from sqlalchemy import func, select

from app.seed import DEMO_CONDUCTOR, DEMO_INSTRUCTOR, DEMO_PASSWORD, seed_if_empty


def demo_login(client, number: str) -> dict[str, str]:
    response = client.post("/api/auth/login", json={"personnel_number": number, "password": DEMO_PASSWORD})
    assert response.status_code == 200, response.text
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def test_seed_creates_consistent_demo_world(client, db):
    from app.engine.models import ScenarioRun
    from app.profiles.models import Employee

    seed_if_empty(db)
    assert db.scalar(select(func.count()).select_from(Employee)) == 43  # 7 бригад × 6 + инструктор
    assert db.scalar(select(func.count()).select_from(ScenarioRun)) > 40

    auth = demo_login(client, DEMO_CONDUCTOR)
    assert client.get("/api/profile", headers=auth).json()["full_name"] == "Смирнов Алексей Андреевич"
    analytics = client.get("/api/analytics/me", headers=auth).json()
    assert analytics["total_runs"] == 4
    assert any("медицинских" in m and "таймер" in m for m in analytics["mistakes"])

    # «Первый рейс» уже есть, а ачивки за конфликт оставлены для живой демонстрации
    profile = client.get("/api/profile", headers=auth).json()
    earned = {a["code"] for a in profile["achievements"] if a["earned_at"]}
    assert "first_trip" in earned and not earned & {"diplomat", "flawless"}

    # Последняя активность 5,5 дня назад — приходит предупреждение о сгорании
    types = [n["type"] for n in client.get("/api/notifications", headers=auth).json()["items"]]
    assert "burn_warning" in types

    board = client.get("/api/leaderboard?scope=company&period=all", headers=auth).json()
    assert board["participants"] == 42
    assert any(row["is_me"] for row in board["rows"])

    team = client.get("/api/analytics/team", headers=demo_login(client, DEMO_INSTRUCTOR)).json()
    assert len(team) == 18  # три бригады депо «Москва»


def test_seed_runs_only_once(db):
    from app.profiles.models import Employee

    seed_if_empty(db)
    seed_if_empty(db)
    assert db.scalar(select(func.count()).select_from(Employee)) == 43
