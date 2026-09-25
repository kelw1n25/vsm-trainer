"""Уведомления и отметка последнего сгорания баллов

Revision ID: 0003
Revises: 0002
Create Date: 2026-09-25
"""

from alembic import op
import sqlalchemy as sa

revision = "0003"
down_revision = "0002"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("employees", sa.Column("last_burn_at", sa.DateTime(timezone=True), nullable=True))
    op.create_table(
        "notifications",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("employee_id", sa.Integer(), sa.ForeignKey("employees.id"), nullable=False),
        sa.Column("type", sa.String(30), nullable=False),
        sa.Column("title", sa.String(200), nullable=False),
        sa.Column("body", sa.Text(), nullable=False),
        sa.Column("dedup_key", sa.String(100), nullable=False),
        sa.Column("read_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint("employee_id", "dedup_key"),
    )
    op.create_index("ix_notifications_employee_created", "notifications", ["employee_id", "created_at"])


def downgrade() -> None:
    op.drop_index("ix_notifications_employee_created", table_name="notifications")
    op.drop_table("notifications")
    op.drop_column("employees", "last_burn_at")
