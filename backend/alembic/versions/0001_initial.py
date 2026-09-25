"""Начальная схема: депо, бригады, сотрудники, сценарии, прохождения, события, ачивки

Revision ID: 0001
Revises:
Create Date: 2026-09-25
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "depots",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("name", sa.String(100), nullable=False, unique=True),
    )
    op.create_table(
        "brigades",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("name", sa.String(100), nullable=False),
        sa.Column("depot_id", sa.Integer(), sa.ForeignKey("depots.id"), nullable=False),
    )
    op.create_table(
        "employees",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("full_name", sa.String(200), nullable=False),
        sa.Column("personnel_number", sa.String(20), nullable=False, unique=True),
        sa.Column("password_hash", sa.String(200), nullable=False),
        sa.Column("role", sa.String(20), nullable=False),
        sa.Column("brigade_id", sa.Integer(), sa.ForeignKey("brigades.id"), nullable=False),
        sa.Column("xp", sa.Integer(), nullable=False),
        sa.Column("competence_points", postgresql.JSONB(), nullable=False),
        sa.Column("last_activity_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_table(
        "scenarios",
        sa.Column("id", sa.String(100), primary_key=True),
        sa.Column("title", sa.String(200), nullable=False),
        sa.Column("category", sa.String(30), nullable=False),
        sa.Column("difficulty", sa.Integer(), nullable=False),
        sa.Column("service_class", sa.String(50), nullable=False),
        sa.Column("route", sa.String(100), nullable=False),
        sa.Column("definition", postgresql.JSONB(), nullable=False),
        sa.Column("content_hash", sa.String(64), nullable=False),
        sa.Column("loaded_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_table(
        "scenario_runs",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("employee_id", sa.Integer(), sa.ForeignKey("employees.id"), nullable=False),
        sa.Column("scenario_id", sa.String(100), sa.ForeignKey("scenarios.id"), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("current_node_id", sa.String(100), nullable=False),
        sa.Column("loyalty", sa.Integer(), nullable=False),
        sa.Column("safety", sa.Integer(), nullable=False),
        sa.Column("flags", postgresql.JSONB(), nullable=False),
        sa.Column("node_entered_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("xp_earned", sa.Integer(), nullable=False),
        sa.Column("competence_points", postgresql.JSONB(), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("finished_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_scenario_runs_employee_finished", "scenario_runs", ["employee_id", "finished_at"])
    op.create_table(
        "events",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("employee_id", sa.Integer(), sa.ForeignKey("employees.id"), nullable=False),
        sa.Column("run_id", sa.Uuid(), sa.ForeignKey("scenario_runs.id"), nullable=True),
        sa.Column("type", sa.String(50), nullable=False),
        sa.Column("payload", postgresql.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_events_employee_created", "events", ["employee_id", "created_at"])
    op.create_index("ix_events_run", "events", ["run_id"])
    op.create_table(
        "employee_achievements",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("employee_id", sa.Integer(), sa.ForeignKey("employees.id"), nullable=False),
        sa.Column("code", sa.String(50), nullable=False),
        sa.Column("run_id", sa.Uuid(), sa.ForeignKey("scenario_runs.id"), nullable=True),
        sa.Column("earned_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint("employee_id", "code"),
    )


def downgrade() -> None:
    op.drop_table("employee_achievements")
    op.drop_index("ix_events_run", table_name="events")
    op.drop_index("ix_events_employee_created", table_name="events")
    op.drop_table("events")
    op.drop_index("ix_scenario_runs_employee_finished", table_name="scenario_runs")
    op.drop_table("scenario_runs")
    op.drop_table("scenarios")
    op.drop_table("employees")
    op.drop_table("brigades")
    op.drop_table("depots")
