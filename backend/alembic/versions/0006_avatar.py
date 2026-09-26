"""Аватар-конструктор сотрудника

Revision ID: 0006
Revises: 0005
Create Date: 2026-09-26
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import JSONB

revision = "0006"
down_revision = "0005"
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Пустой объект — аватар по умолчанию: у всех, кто ещё не настраивал, он остаётся прежним
    op.add_column("employees", sa.Column("avatar", JSONB(), server_default=sa.text("'{}'::jsonb"), nullable=False))


def downgrade() -> None:
    op.drop_column("employees", "avatar")
