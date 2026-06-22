"""Add missing user verification columns for existing databases."""

from alembic import op


revision = "20260622_0002"
down_revision = "20260606_0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS student_email_domain VARCHAR(120)")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_document_type VARCHAR(40)")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_reviewed_at TIMESTAMPTZ")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_reviewed_by UUID")
    op.execute(
        "DO $$ BEGIN ALTER TABLE users ADD CONSTRAINT fk_users_verification_reviewed_by "
        "FOREIGN KEY (verification_reviewed_by) REFERENCES users(id) ON DELETE SET NULL; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )


def downgrade() -> None:
    op.execute(
        "DO $$ BEGIN ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_verification_reviewed_by; "
        "EXCEPTION WHEN undefined_table THEN NULL; END $$"
    )
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS verification_reviewed_by")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS verification_reviewed_at")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS verification_document_type")
    op.execute("ALTER TABLE users DROP COLUMN IF EXISTS student_email_domain")
