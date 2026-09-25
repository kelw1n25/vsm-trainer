import logging
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.db import SessionLocal, get_db
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
app.include_router(scenarios_router)


@app.get("/api/health", tags=["system"])
def health(db: Session = Depends(get_db)) -> dict[str, str]:
    db.execute(text("SELECT 1"))
    return {"status": "ok"}
