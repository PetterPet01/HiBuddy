"""Multi-assignee tasks: introduce task_assignments and migrate assignee_id."""
from alembic import op

revision = "20260622_0002"
down_revision = "20260606_0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto")
    op.execute(
        """
        CREATE TABLE IF NOT EXISTS task_assignments (
            id UUID PRIMARY KEY,
            task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
            assignee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT uq_task_assignment UNIQUE (task_id, assignee_id)
        )
        """
    )
    op.execute(
        "CREATE INDEX IF NOT EXISTS ix_task_assignments_task_id ON task_assignments (task_id)"
    )
    op.execute(
        "CREATE INDEX IF NOT EXISTS ix_task_assignments_assignee_id ON task_assignments (assignee_id)"
    )

    # Backfill one assignment row per existing task that still carries assignee_id.
    op.execute(
        """
        INSERT INTO task_assignments (id, task_id, assignee_id, assigned_at)
        SELECT gen_random_uuid(), t.id, t.assignee_id, COALESCE(t.created_at, now())
        FROM tasks t
        WHERE t.assignee_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM task_assignments ta
              WHERE ta.task_id = t.id AND ta.assignee_id = t.assignee_id
          )
        """
    )

    op.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS assignee_id")


def downgrade() -> None:
    op.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assignee_id UUID")
    # Restore the earliest assignment as the single legacy assignee.
    op.execute(
        """
        UPDATE tasks t
        SET assignee_id = sub.assignee_id
        FROM (
            SELECT DISTINCT ON (task_id) task_id, assignee_id
            FROM task_assignments
            ORDER BY task_id, assigned_at ASC
        ) sub
        WHERE sub.task_id = t.id
        """
    )
    op.execute("DROP TABLE IF EXISTS task_assignments")
