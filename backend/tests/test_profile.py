from io import BytesIO

from PIL import Image


def test_avatar_defaults_until_customized(client, auth):
    assert client.get("/api/profile", headers=auth).json()["avatar"] == {"background": "blue", "headwear": "cap", "tie": "red"}


def test_avatar_is_saved_and_returned_in_profile(client, auth):
    chosen = {"background": "night", "headwear": "none", "tie": "green"}
    response = client.put("/api/profile/avatar", json=chosen, headers=auth)
    assert response.status_code == 200
    assert response.json() == chosen
    assert client.get("/api/profile", headers=auth).json()["avatar"] == chosen


def test_avatar_accepts_only_constructor_values(client, auth):
    # Конструктор принимает только свои варианты; фото загружается отдельно — PUT /api/profile/avatar/photo
    unknown = client.put("/api/profile/avatar", json={"background": "https://example.com/photo.jpg"}, headers=auth)
    assert unknown.status_code == 422
    extra = client.put("/api/profile/avatar", json={"photo": "base64"}, headers=auth)
    assert extra.status_code == 422
    assert client.get("/api/profile", headers=auth).json()["avatar"]["background"] == "blue"


def test_avatar_requires_login(client):
    assert client.put("/api/profile/avatar", json={}).status_code == 401


PHOTO_URL = "/api/profile/avatar/photo"


def _image(fmt: str = "JPEG", size: tuple[int, int] = (800, 600), exif: bool = False) -> bytes:
    image = Image.new("RGB", size, (200, 40, 40))
    out = BytesIO()
    if exif:
        tags = Image.Exif()
        tags[0x010F] = "Камера сотрудника"  # Make
        tags[0x8825] = {1: "N", 2: (55.0, 45.0, 0.0)}  # GPS
        image.save(out, fmt, exif=tags)
    else:
        image.save(out, fmt)
    return out.getvalue()


def _upload(client, auth, body: bytes, content_type: str = "image/jpeg", consent: bool = True):
    return client.put(PHOTO_URL, params={"consent": consent}, content=body, headers={**auth, "Content-Type": content_type})


def test_no_photo_until_uploaded(client, auth):
    assert client.get("/api/profile", headers=auth).json()["avatar_photo"] is None
    assert client.get(PHOTO_URL, headers=auth).json()["detail"]["code"] == "photo_not_found"


def test_photo_is_square_jpeg_without_metadata(client, auth):
    response = _upload(client, auth, _image("PNG", exif=True), "image/png")
    assert response.status_code == 200
    version = response.json()["version"]
    assert client.get("/api/profile", headers=auth).json()["avatar_photo"] == version

    stored = client.get(PHOTO_URL, headers=auth)
    assert stored.headers["content-type"] == "image/jpeg"
    with Image.open(BytesIO(stored.content)) as image:
        assert image.format == "JPEG"
        assert image.size == (512, 512)
        # Ни камеры, ни геометки: файл пересобран, а не сохранён как прислали
        assert dict(image.getexif()) == {}


def test_photo_requires_consent(client, auth):
    response = _upload(client, auth, _image(), consent=False)
    assert response.status_code == 400
    assert response.json()["detail"]["code"] == "consent_required"
    assert client.get("/api/profile", headers=auth).json()["avatar_photo"] is None


def test_photo_rejects_not_images_and_large_files(client, auth):
    fake = _upload(client, auth, b"<script>alert(1)</script>")
    assert fake.status_code == 422
    assert fake.json()["detail"]["code"] == "photo_invalid"
    gif = BytesIO()
    Image.new("RGB", (10, 10)).save(gif, "GIF")
    assert _upload(client, auth, gif.getvalue(), "image/gif").json()["detail"]["code"] == "photo_invalid"
    assert _upload(client, auth, b"").json()["detail"]["code"] == "photo_empty"
    huge = _upload(client, auth, b"0" * (8 * 1024 * 1024 + 1))
    assert huge.status_code == 413
    assert huge.json()["detail"]["code"] == "photo_too_large"


def test_new_photo_replaces_old_and_delete_removes_it(client, auth):
    first = _upload(client, auth, _image()).json()["version"]
    second = _upload(client, auth, _image(size=(300, 900))).json()["version"]
    assert second >= first
    assert client.delete(PHOTO_URL, headers=auth).status_code == 204
    assert client.get("/api/profile", headers=auth).json()["avatar_photo"] is None
    assert client.get(PHOTO_URL, headers=auth).status_code == 404


def test_photo_requires_login(client):
    assert client.get(PHOTO_URL).status_code == 401
    assert client.put(PHOTO_URL, params={"consent": True}, content=_image()).status_code == 401
