from fastapi.testclient import TestClient


def test_unexpected_error_hides_details(db, monkeypatch, auth):
    from app.auth import deps
    from app.main import app

    def broken(_token):
        raise RuntimeError("секретная деталь реализации")

    monkeypatch.setattr(deps, "decode_token", broken)
    response = TestClient(app, raise_server_exceptions=False).get("/api/profile", headers=auth)
    assert response.status_code == 500
    body = response.json()["detail"]
    assert body["code"] == "internal_error"
    assert "секретная" not in response.text
    assert response.headers["X-Request-ID"] in body["message"]


def test_request_id_is_echoed(client):
    response = client.get("/api/health", headers={"X-Request-ID": "mobile-12345678"})
    assert response.headers["X-Request-ID"] == "mobile-12345678"
    # Неподходящий идентификатор заменяется своим
    assert client.get("/api/health", headers={"X-Request-ID": "bad id!"}).headers["X-Request-ID"] != "bad id!"


def test_invalid_input_gets_readable_message(client, auth):
    # Тело — не JSON: сообщение без внутренних позиций символов
    broken = client.post("/api/runs", content=b"{bad", headers={**auth, "Content-Type": "application/json"})
    assert broken.status_code == 422
    assert broken.json()["detail"] == {"code": "validation_error", "message": "Тело запроса — некорректный JSON"}
    # Не хватает поля: названо, какого
    missing = client.post("/api/runs", json={}, headers=auth)
    assert missing.status_code == 422
    assert "scenario_id" in missing.json()["detail"]["message"]
