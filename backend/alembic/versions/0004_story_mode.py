"""Режим новеллы: скрытые параметры прохождения и момент показа вариантов

Revision ID: 0004
Revises: 0003
Create Date: 2026-09-25
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import JSONB

revision = "0004"
down_revision = "0003"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("scenario_runs", sa.Column("stats", JSONB(), server_default="{}", nullable=False))
    op.add_column("scenario_runs", sa.Column("choices_shown_at", sa.DateTime(timezone=True), nullable=True))


def downgrade() -> None:
    op.drop_column("scenario_runs", "choices_shown_at")
    op.drop_column("scenario_runs", "stats")
