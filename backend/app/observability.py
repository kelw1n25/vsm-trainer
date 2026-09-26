"""Журналы в JSON и сквозной идентификатор запроса.

Каждая строка журнала — один JSON-объект: её разбирают сборщики логов без регулярных выражений.
request_id приходит от клиента в X-Request-ID (или создаётся здесь), возвращается в ответе
и попадает во все строки, записанные во время запроса, — по нему находят ошибку, о которой сообщил пользователь.
Персональные данные (ФИО, табельные номера, пароли, токены) в журнал не пишутся.
"""

import json
import logging
import re
import time
import uuid
from contextvars import ContextVar

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.types import ASGIApp, Message, Receive, Scope, Send

request_id: ContextVar[str] = ContextVar("request_id", default="-")
_CLIENT_ID = re.compile(r"^[A-Za-z0-9-]{8,64}$")
# Поля, которые есть у любой записи logging, — всё остальное пришло через extra=
_STANDARD = set(vars(logging.makeLogRecord({}))) | {"message", "asctime"}
log = logging.getLogger("app")


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry = {
            "ts": self.formatTime(record, "%Y-%m-%dT%H:%M:%S"),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "request_id": request_id.get(),
        }
        entry.update({key: value for key, value in vars(record).items() if key not in _STANDARD})
        if record.exc_info:
            entry["exception"] = self.formatException(record.exc_info)
        return json.dumps(entry, ensure_ascii=False, default=str)


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter())
    logging.basicConfig(level=logging.INFO, handlers=[handler], force=True)
    # Строки доступа uvicorn дублируют наши и содержат query-параметры — отключаем
    logging.getLogger("uvicorn.access").disabled = True


class RequestContextMiddleware:
    """Выдаёт request_id, пишет по строке на запрос: метод, путь без параметров, статус, длительность."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        incoming = dict(scope["headers"]).get(b"x-request-id", b"").decode("latin-1")
        rid = incoming if _CLIENT_ID.match(incoming) else uuid.uuid4().hex
        token = request_id.set(rid)
        started = time.perf_counter()
        status = 500

        async def send_with_id(message: Message) -> None:
            nonlocal status
            if message["type"] == "http.response.start":
                status = message["status"]
                message["headers"] = [*message.get("headers", []), (b"x-request-id", rid.encode())]
            await send(message)

        try:
            await self.app(scope, receive, send_with_id)
        finally:
            log.info("request", extra={
                "method": scope["method"],
                "path": scope["path"],
                "status": status,
                "duration_ms": round((time.perf_counter() - started) * 1000, 1),
            })
            request_id.reset(token)


async def unhandled_error_handler(_: Request, error: Exception) -> JSONResponse:
    """Непредвиденная ошибка: стек — в журнал, пользователю — код и request_id для обращения в поддержку."""
    log.error("unhandled_error", exc_info=error)
    rid = request_id.get()
    return JSONResponse(
        status_code=500,
        content={"detail": {
            "code": "internal_error",
            "message": f"Внутренняя ошибка сервера. Если повторится, сообщите код {rid}",
        }},
        headers={"X-Request-ID": rid},
    )
