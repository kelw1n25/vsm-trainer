"""Причина завершения прохождения: финальный узел или обнуление шкалы

Revision ID: 0002
Revises: 0001
Create Date: 2026-09-25
"""

from alembic import op
import sqlalchemy as sa

revision = "0002"
down_revision = "0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("scenario_runs", sa.Column("finish_reason", sa.String(30), nullable=True))


def downgrade() -> None:
    op.drop_column("scenario_runs", "finish_reason")
