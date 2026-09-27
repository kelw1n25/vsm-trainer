import uuid
from datetime import datetime

from fastapi import APIRouter, Depends, Query, Request, Response
from fastapi.concurrency import run_in_threadpool
from pydantic import BaseModel
from sqlalchemy import delete, func, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.achievements.models import EmployeeAchievement
from app.achievements.service import AchievementOut, LevelOut, describe, level_for
from app.auth.deps import get_current_employee
from app.db import get_db
from app.errors import api_error
from app.engine.models import RunStatus, ScenarioRun
from app.game_config import game_config
from app.profiles.avatar import Avatar, avatar_of
from app.profiles.models import Employee, EmployeePhoto, Role
from app.profiles.photo import MAX_UPLOAD_BYTES, PhotoError, normalize
from app.scenarios.models import Scenario

router = APIRouter(prefix="/api/profile", tags=["profile"])

HISTORY_LIMIT = 20


class ProfileAchievement(AchievementOut):
    earned_at: datetime | None


class HistoryItem(BaseModel):
    run_id: uuid.UUID
    scenario_id: str
    scenario_title: str
    category: str
    outcome: RunStatus
    xp_earned: int
    loyalty: int
    safety: int
    finished_at: datetime


class Profile(BaseModel):
    id: int
    full_name: str
    personnel_number: str
    role: Role
    brigade: str
    depot: str
    level: LevelOut
    runs_completed: int
    competence_points: dict[str, int]
    avatar: Avatar
    # Версия своего фото (метка времени загрузки) — клиент берёт GET /api/profile/avatar/photo?v=<версия>;
    # null — фото нет, рисуется аватар-конструктор
    avatar_photo: int | None
    achievements: list[ProfileAchievement]
    history: list[HistoryItem]


@router.get("", summary="Профиль текущего сотрудника: уровень, компетенции, ачивки, история")
def get_profile(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> Profile:
    earned = {
        a.code: a.earned_at
        for a in db.scalars(select(EmployeeAchievement).where(EmployeeAchievement.employee_id == employee.id))
    }
    runs = db.execute(
        select(ScenarioRun, Scenario)
        .join(Scenario, Scenario.id == ScenarioRun.scenario_id)
        .where(ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS)
        .order_by(ScenarioRun.finished_at.desc())
        .limit(HISTORY_LIMIT)
    )
    return Profile(
        id=employee.id,
        full_name=employee.full_name,
        personnel_number=employee.personnel_number,
        role=employee.role,
        brigade=employee.brigade.name,
        depot=employee.brigade.depot.name,
        level=level_for(employee.xp),
        runs_completed=db.scalar(
            select(func.count()).where(
                ScenarioRun.employee_id == employee.id, ScenarioRun.status != RunStatus.IN_PROGRESS
            )
        ),
        competence_points={code: employee.competence_points.get(code, 0) for code in game_config.competences},
        avatar=avatar_of(employee.avatar),
        avatar_photo=photo_version(db, employee.id),
        # Все ачивки из конфига: полученные — с датой, остальные — как цель
        achievements=[
            ProfileAchievement(**describe(code).model_dump(), earned_at=earned.get(code))
            for code in game_config.achievements
        ],
        history=[
            HistoryItem(
                run_id=run.id,
                scenario_id=scenario.id,
                scenario_title=scenario.title,
                category=scenario.category,
                outcome=run.status,
                xp_earned=run.xp_earned,
                loyalty=run.loyalty,
                safety=run.safety,
                finished_at=run.finished_at,
            )
            for run, scenario in runs
        ],
    )


@router.put("/avatar", summary="Сохранить аватар: фон, головной убор и галстук из готовых вариантов")
def update_avatar(
    avatar: Avatar, employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)
) -> Avatar:
    employee.avatar = avatar.model_dump()
    db.commit()
    return avatar


class AvatarPhoto(BaseModel):
    version: int


def photo_version(db: Session, employee_id: int) -> int | None:
    updated_at = db.scalar(select(EmployeePhoto.updated_at).where(EmployeePhoto.employee_id == employee_id))
    return int(updated_at.timestamp() * 1000) if updated_at else None


@router.put(
    "/avatar/photo",
    summary="Поставить своё фото на аватар (тело — JPEG, PNG или WebP до 8 МБ; нужен consent=true)",
    openapi_extra={
        "requestBody": {
            "required": True,
            "content": {t: {"schema": {"type": "string", "format": "binary"}} for t in ("image/jpeg", "image/png", "image/webp")},
        }
    },
)
async def upload_photo(
    request: Request,
    consent: bool = Query(False, description="Сотрудник согласился на обработку своего фото"),
    employee: Employee = Depends(get_current_employee),
    db: Session = Depends(get_db),
) -> AvatarPhoto:
    if not consent:
        raise api_error(400, "consent_required", "Подтвердите согласие на обработку фотографии")
    declared = request.headers.get("content-length")
    if declared and declared.isdigit() and int(declared) > MAX_UPLOAD_BYTES:
        raise api_error(413, "photo_too_large", "Фотография больше 8 МБ — выберите снимок поменьше")
    data = await request.body()
    if not data:
        raise api_error(400, "photo_empty", "Файл пустой — выберите фотографию")
    if len(data) > MAX_UPLOAD_BYTES:
        raise api_error(413, "photo_too_large", "Фотография больше 8 МБ — выберите снимок поменьше")
    try:
        # Распаковка и сжатие — работа процессора: вне цикла событий, чтобы не задерживать другие запросы
        image = await run_in_threadpool(normalize, data)
    except PhotoError as error:
        raise api_error(422, "photo_invalid", str(error)) from error
    statement = insert(EmployeePhoto).values(employee_id=employee.id, image=image)
    db.execute(
        statement.on_conflict_do_update(
            index_elements=[EmployeePhoto.employee_id], set_={"image": image, "updated_at": func.now()}
        )
    )
    db.commit()
    return AvatarPhoto(version=photo_version(db, employee.id))


@router.get(
    "/avatar/photo",
    summary="Своё фото на аватаре (JPEG 512 × 512) — только самому сотруднику",
    responses={200: {"content": {"image/jpeg": {}}}},
    response_class=Response,
)
def get_photo(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> Response:
    image = db.scalar(select(EmployeePhoto.image).where(EmployeePhoto.employee_id == employee.id))
    if image is None:
        raise api_error(404, "photo_not_found", "Фото не загружено")
    # Адрес с ?v=<версия> меняется при каждой загрузке — кэш не покажет старое фото
    return Response(image, media_type="image/jpeg", headers={"Cache-Control": "private, max-age=86400"})


@router.delete("/avatar/photo", status_code=204, summary="Убрать своё фото: оно удаляется с сервера")
def delete_photo(employee: Employee = Depends(get_current_employee), db: Session = Depends(get_db)) -> Response:
    db.execute(delete(EmployeePhoto).where(EmployeePhoto.employee_id == employee.id))
    db.commit()
    return Response(status_code=204)
