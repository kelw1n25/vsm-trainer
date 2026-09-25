import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.exceptions import RequestValidationError
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.analytics.router import router as analytics_router
from app.auth.router import router as auth_router
from app.config import settings
from app.db import SessionLocal, get_db
from app.engine.router import router as runs_router
from app.errors import validation_error_handler
from app.game_config import game_config
from app.integration.router import router as integration_router
from app.leaderboard.router import router as leaderboard_router
from app.notifications.router import router as notifications_router
from app.profiles.router import router as profile_router
from app.scenarios.loader import load_scenarios
from app.scenarios.router import router as scenarios_router
from app.seed import seed_if_empty

logging.basicConfig(level=logging.INFO, format="%(levelname)s [%(name)s] %(message)s")


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    with SessionLocal() as db:
        load_scenarios(db)
        if settings.seed_demo_data:
            seed_if_empty(db)
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
app.include_router(leaderboard_router)
app.include_router(notifications_router)
app.include_router(analytics_router)
app.include_router(integration_router)


@app.get("/api/health", tags=["system"])
def health(db: Session = Depends(get_db)) -> dict[str, str]:
    db.execute(text("SELECT 1"))
    return {"status": "ok"}


@app.get("/api/meta", tags=["system"], summary="Справочники для интерфейса: названия компетенций")
def meta() -> dict[str, dict[str, str]]:
    return {"competences": game_config.competences}
