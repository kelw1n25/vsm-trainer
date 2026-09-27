"""Своё фото на аватаре: проверка и обработка загруженного снимка.

Фото сотрудника — персональные данные (152-ФЗ): загружается только с согласия, видно только самому сотруднику
(в рейтинге у остальных — инициалы) и удаляется одной кнопкой. Сервер не хранит присланный файл как есть:
картинка поворачивается по EXIF, обрезается в квадрат и сохраняется заново как JPEG — без EXIF, геометки
и названия камеры. Файл, который не открывается как JPEG / PNG / WebP, отклоняется.
"""

import warnings
from io import BytesIO

from PIL import Image, ImageOps, UnidentifiedImageError

MAX_UPLOAD_BYTES = 8 * 1024 * 1024
SIDE = 512
FORMATS = {"JPEG", "PNG", "WEBP"}
# Снимок телефона — 12–50 Мп; больше — не фото, а «бомба» распаковки
Image.MAX_IMAGE_PIXELS = 60_000_000


class PhotoError(ValueError):
    """Файл не подходит как фото аватара; текст — для сотрудника."""


def normalize(data: bytes) -> bytes:
    """Квадрат SIDE × SIDE в JPEG без метаданных."""
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(BytesIO(data)) as image:
                if image.format not in FORMATS:
                    raise PhotoError("Нужна фотография в формате JPEG, PNG или WebP")
                image.load()
                upright = ImageOps.exif_transpose(image)
                square = ImageOps.fit(upright.convert("RGB"), (SIDE, SIDE), Image.Resampling.LANCZOS)
    except (UnidentifiedImageError, Image.DecompressionBombError, Image.DecompressionBombWarning, OSError) as error:
        raise PhotoError("Не удалось открыть файл как фотографию: нужен JPEG, PNG или WebP") from error
    out = BytesIO()
    square.save(out, "JPEG", quality=85, optimize=True)
    return out.getvalue()
