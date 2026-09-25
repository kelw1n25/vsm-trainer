from fastapi import HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


def api_error(status: int, code: str, message: str) -> HTTPException:
    """Единый формат ошибок API: {"detail": {"code": ..., "message": ...}}.

    code — для программ (фронтенд, HR/LMS), message — для человека.
    """
    return HTTPException(status_code=status, detail={"code": code, "message": message})


async def validation_error_handler(_: Request, error: RequestValidationError) -> JSONResponse:
    fields = [".".join(str(part) for part in item["loc"] if part != "body") for item in error.errors()]
    return JSONResponse(
        status_code=422,
        content={
            "detail": {
                "code": "validation_error",
                "message": f"Некорректные данные запроса. Проверьте поля: {', '.join(fields)}",
            }
        },
    )
