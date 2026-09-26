from fastapi import HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


def api_error(status: int, code: str, message: str, headers: dict[str, str] | None = None) -> HTTPException:
    """Единый формат ошибок API: {"detail": {"code": ..., "message": ...}}.

    code — для программ (веб- и мобильные клиенты, HR/LMS), message — для человека.
    """
    return HTTPException(status_code=status, detail={"code": code, "message": message}, headers=headers)


async def validation_error_handler(_: Request, error: RequestValidationError) -> JSONResponse:
    errors = error.errors()
    # Тело — не JSON: позиция символа в ответе человеку ничего не скажет
    if any(item["type"] == "json_invalid" for item in errors):
        message = "Тело запроса — некорректный JSON"
    else:
        fields = [".".join(str(part) for part in item["loc"] if part != "body") for item in errors]
        message = f"Некорректные данные запроса. Проверьте поля: {', '.join(fields)}"
    return JSONResponse(status_code=422, content={"detail": {"code": "validation_error", "message": message}})
