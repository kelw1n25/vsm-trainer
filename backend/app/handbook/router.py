from fastapi import APIRouter, Depends

from app.auth.deps import get_current_employee
from app.handbook.data import Handbook, handbook

router = APIRouter(prefix="/api/handbook", tags=["handbook"])


@router.get(
    "",
    summary="Справочник: ситуации на борту, ролевая модель, классы обслуживания",
    dependencies=[Depends(get_current_employee)],
)
def get_handbook() -> Handbook:
    return handbook
