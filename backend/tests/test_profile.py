def test_avatar_defaults_until_customized(client, auth):
    assert client.get("/api/profile", headers=auth).json()["avatar"] == {"background": "blue", "headwear": "cap", "tie": "red"}


def test_avatar_is_saved_and_returned_in_profile(client, auth):
    chosen = {"background": "night", "headwear": "none", "tie": "green"}
    response = client.put("/api/profile/avatar", json=chosen, headers=auth)
    assert response.status_code == 200
    assert response.json() == chosen
    assert client.get("/api/profile", headers=auth).json()["avatar"] == chosen


def test_avatar_accepts_only_constructor_values(client, auth):
    # Фото и произвольные значения не принимаются: только варианты конструктора
    unknown = client.put("/api/profile/avatar", json={"background": "https://example.com/photo.jpg"}, headers=auth)
    assert unknown.status_code == 422
    extra = client.put("/api/profile/avatar", json={"photo": "base64"}, headers=auth)
    assert extra.status_code == 422
    assert client.get("/api/profile", headers=auth).json()["avatar"]["background"] == "blue"


def test_avatar_requires_login(client):
    assert client.put("/api/profile/avatar", json={}).status_code == 401
