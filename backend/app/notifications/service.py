from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.notifications.models import Notification


def notify(db: Session, employee_id: int, type_: str, title: str, body: str, dedup_key: str) -> None:
    """Создаёт уведомление, если такого (по dedup_key) у сотрудника ещё нет."""
    db.execute(
        insert(Notification)
        .values(employee_id=employee_id, type=type_, title=title, body=body, dedup_key=dedup_key)
        .on_conflict_do_nothing()
    )
