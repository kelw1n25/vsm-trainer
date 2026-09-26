from tests.conftest import create_employee


def sign_in(client, password: str = "secret"):
    return client.post("/api/auth/login", json={"personnel_number": "100001", "password": password})


def refresh(client, token: str):
    return client.post("/api/auth/refresh", json={"refresh_token": token})


def test_login_returns_token_pair(client, employee):
    session = sign_in(client).json()
    assert session["refresh_token"] and session["expires_in"] == 720 * 60
    profile = client.get("/api/profile", headers={"Authorization": f"Bearer {session['access_token']}"})
    assert profile.status_code == 200


def test_refresh_rotates_token(client, employee):
    first = sign_in(client).json()["refresh_token"]
    renewed = refresh(client, first)
    assert renewed.status_code == 200
    second = renewed.json()["refresh_token"]
    assert second != first
    assert client.get("/api/profile", headers={"Authorization": f"Bearer {renewed.json()['access_token']}"}).status_code == 200
    # Использованный токен второй раз не принимается
    assert refresh(client, first).status_code == 401


def test_reused_refresh_token_revokes_all_sessions(client, employee):
    stolen = sign_in(client).json()["refresh_token"]
    legitimate = refresh(client, stolen).json()["refresh_token"]
    other_device = sign_in(client).json()["refresh_token"]

    # Кто-то предъявил уже обменянный токен — значит, его скопировали
    reuse = refresh(client, stolen)
    assert reuse.status_code == 401
    assert reuse.json()["detail"]["code"] == "invalid_refresh_token"
    assert refresh(client, legitimate).status_code == 401
    assert refresh(client, other_device).status_code == 401


def test_logout_revokes_only_this_device(client, employee):
    phone = sign_in(client).json()["refresh_token"]
    tablet = sign_in(client).json()["refresh_token"]
    assert client.post("/api/auth/logout", json={"refresh_token": phone}).status_code == 200
    assert refresh(client, phone).status_code == 401
    assert refresh(client, tablet).status_code == 200
    # Повторный выход ничего не ломает
    assert client.post("/api/auth/logout", json={"refresh_token": phone}).status_code == 200


def test_unknown_refresh_token(client):
    response = refresh(client, "forged")
    assert response.status_code == 401
    assert response.json()["detail"]["code"] == "invalid_refresh_token"


def test_repeated_failures_are_rate_limited(client, employee):
    for _ in range(5):
        assert sign_in(client, "wrong").status_code == 401
    blocked = sign_in(client)  # даже с верным паролем, пока окно не прошло
    assert blocked.status_code == 429
    assert blocked.json()["detail"]["code"] == "too_many_attempts"
    assert int(blocked.headers["Retry-After"]) > 0


def test_successful_login_resets_failures(client, employee):
    for _ in range(4):
        sign_in(client, "wrong")
    assert sign_in(client).status_code == 200
    for _ in range(4):
        sign_in(client, "wrong")
    assert sign_in(client).status_code == 200


def test_rate_limit_per_ip_covers_different_numbers(client, db):
    for number in range(20):
        client.post("/api/auth/login", json={"personnel_number": f"9{number:05}", "password": "x"})
    create_employee(db, "100001")
    assert sign_in(client).status_code == 429
