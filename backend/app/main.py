import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.exceptions import RequestValidationError
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.auth.router import router as auth_router
from app.db import SessionLocal, get_db
from app.engine.router import router as runs_router
from app.errors import validation_error_handler
from app.game_config import game_config
from app.profiles.router import router as profile_router
from app.scenarios.loader import load_scenarios
from app.scenarios.router import router as scenarios_router

logging.basicConfig(level=logging.INFO, format="%(levelname)s [%(name)s] %(message)s")


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    with SessionLocal() as db:
        load_scenarios(db)
    yield


app = FastAPI(
    title="ВСМ-тренажёр проводника",
    description="Нелинейные сценарии с таймером, шкалами и очками компетенций.",
    version="0.1.0",
    docs_url="/api/docs",
    openapi_url="/api/openapi.json",
    lifespan=lifespan,
)
app.add_exception_handler(RequestValidationError, validation_error_handler)
app.include_router(auth_router)
app.include_router(scenarios_router)
app.include_router(runs_router)
app.include_router(profile_router)


@app.get("/api/health", tags=["system"])
def health(db: Session = Depends(get_db)) -> dict[str, str]:
    db.execute(text("SELECT 1"))
    return {"status": "ok"}


@app.get("/api/meta", tags=["system"], summary="Справочники для интерфейса: названия компетенций")
def meta() -> dict[str, dict[str, str]]:
    return {"competences": game_config.competences}
