"""Baseline the pre-migration HiBuddy schema."""
from alembic import op
from app.database import Base
import app.models  # noqa: F401

revision = "20260606_0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    Base.metadata.create_all(bind=op.get_bind())
    op.execute("ALTER TABLE users ALTER COLUMN hashed_password DROP NOT NULL")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20)")
    op.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_submitted_at TIMESTAMPTZ")

    op.execute("ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS jti_hash VARCHAR(64)")
    op.execute("ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_family VARCHAR(36)")
    op.execute("ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS device_name VARCHAR(120)")
    op.execute("ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_jti_hash VARCHAR(64)")
    op.execute("ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS ix_refresh_tokens_jti_hash ON refresh_tokens (jti_hash)")

    op.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS client_message_id VARCHAR(128)")
    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_message_sender_client_id "
        "ON messages (sender_id, client_message_id) WHERE client_message_id IS NOT NULL"
    )

    op.execute("ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS catalog_role_id UUID")
    op.execute(
        "DO $$ BEGIN ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_catalog "
        "FOREIGN KEY (catalog_role_id) REFERENCES role_catalog(id) ON DELETE SET NULL; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )

    op.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS moderation_categories JSON")
    op.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS moderation_reasons JSON")
    op.execute("ALTER TABLE projects ADD COLUMN IF NOT EXISTS moderation_checked_at TIMESTAMPTZ")

    op.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS evidence_url VARCHAR(500)")
    op.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS context_type VARCHAR(40)")
    op.execute("ALTER TABLE reports ADD COLUMN IF NOT EXISTS context_id VARCHAR(36)")

    op.execute("ALTER TABLE matches ADD COLUMN IF NOT EXISTS score_explanation JSON")
    op.execute("ALTER TABLE swipe_actions ADD COLUMN IF NOT EXISTS context_project_id UUID")
    op.execute("ALTER TABLE swipe_actions ADD COLUMN IF NOT EXISTS context_role_slot_id UUID")
    op.execute("ALTER TABLE swipe_actions ADD COLUMN IF NOT EXISTS context_key VARCHAR(80)")
    op.execute("UPDATE swipe_actions SET context_key = 'GLOBAL' WHERE context_key IS NULL")
    op.execute("ALTER TABLE swipe_actions ALTER COLUMN context_key SET DEFAULT 'GLOBAL'")
    op.execute("ALTER TABLE swipe_actions ALTER COLUMN context_key SET NOT NULL")
    op.execute(
        "DO $$ BEGIN ALTER TABLE swipe_actions ADD CONSTRAINT fk_swipe_context_project "
        "FOREIGN KEY (context_project_id) REFERENCES projects(id) ON DELETE CASCADE; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )
    op.execute(
        "DO $$ BEGIN ALTER TABLE swipe_actions ADD CONSTRAINT fk_swipe_context_slot "
        "FOREIGN KEY (context_role_slot_id) REFERENCES project_role_slots(id) ON DELETE SET NULL; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )

    op.execute("ALTER TABLE swipe_queue_items ADD COLUMN IF NOT EXISTS context_project_id UUID")
    op.execute("ALTER TABLE swipe_queue_items ADD COLUMN IF NOT EXISTS context_key VARCHAR(80)")
    op.execute("UPDATE swipe_queue_items SET context_key = 'GLOBAL' WHERE context_key IS NULL")
    op.execute("ALTER TABLE swipe_queue_items ALTER COLUMN context_key SET DEFAULT 'GLOBAL'")
    op.execute("ALTER TABLE swipe_queue_items ALTER COLUMN context_key SET NOT NULL")
    op.execute(
        "DO $$ BEGIN ALTER TABLE swipe_queue_items ADD CONSTRAINT fk_queue_context_project "
        "FOREIGN KEY (context_project_id) REFERENCES projects(id) ON DELETE CASCADE; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )

    op.execute(
        "UPDATE project_role_slots SET skill_requirements = converted.requirements "
        "FROM ("
        "  SELECT id, COALESCE(("
        "    SELECT json_object_agg(trim(skill), 'BEGINNER') "
        "    FROM regexp_split_to_table(skill_requirements->>'requirements', ',') AS skill "
        "    WHERE trim(skill) <> ''"
        "  ), '{}'::json) AS requirements "
        "  FROM project_role_slots "
        "  WHERE skill_requirements IS NOT NULL AND skill_requirements->>'requirements' IS NOT NULL"
        ") AS converted WHERE project_role_slots.id = converted.id"
    )
    op.execute(
        "UPDATE project_role_slots SET skill_requirements = NULL "
        "WHERE skill_requirements IS NOT NULL "
        "AND json_typeof(skill_requirements) IS DISTINCT FROM 'object'"
    )

    op.execute(
        "INSERT INTO skill_catalog (id, slug, name, is_active) "
        "SELECT gen_random_uuid(), "
        "trim(both '-' from regexp_replace(lower(skill_name), '[^a-z0-9]+', '-', 'g')), "
        "min(skill_name), true "
        "FROM user_skills GROUP BY lower(skill_name) "
        "ON CONFLICT DO NOTHING"
    )
    op.execute(
        "INSERT INTO user_role_skills "
        "(id, user_role_id, skill_id, level, needs_improvement, created_at) "
        "SELECT gen_random_uuid(), ur.id, sc.id, us.level, us.needs_improvement, now() "
        "FROM user_roles ur "
        "JOIN user_skills us ON us.user_id = ur.user_id "
        "JOIN skill_catalog sc ON lower(sc.name) = lower(us.skill_name) "
        "ON CONFLICT DO NOTHING"
    )
    op.execute(
        "INSERT INTO skill_catalog (id, slug, name, is_active) "
        "SELECT gen_random_uuid(), "
        "trim(both '-' from regexp_replace(lower(req.skill_name), '[^a-z0-9]+', '-', 'g')), "
        "min(req.skill_name), true "
        "FROM ("
        " SELECT json_object_keys(skill_requirements) AS skill_name "
        " FROM project_role_slots "
        " WHERE skill_requirements IS NOT NULL AND json_typeof(skill_requirements) = 'object'"
        ") req GROUP BY lower(req.skill_name) ON CONFLICT DO NOTHING"
    )
    op.execute(
        "INSERT INTO project_role_skill_requirements "
        "(id, role_slot_id, skill_id, minimum_level, is_required) "
        "SELECT gen_random_uuid(), prs.id, sc.id, "
        "upper(COALESCE(prs.skill_requirements->>key_name, 'BEGINNER')), true "
        "FROM project_role_slots prs "
        "CROSS JOIN LATERAL json_object_keys(prs.skill_requirements) AS key_name "
        "JOIN skill_catalog sc ON lower(sc.name) = lower(key_name) "
        "WHERE prs.skill_requirements IS NOT NULL "
        "AND json_typeof(prs.skill_requirements) = 'object' "
        "ON CONFLICT DO NOTHING"
    )

    op.execute(
        "WITH ranked AS ("
        " SELECT id, row_number() OVER ("
        "  PARTITION BY swiper_id, target_type, target_id, context_key "
        "  ORDER BY created_at DESC, id DESC"
        " ) AS rn FROM swipe_actions WHERE is_active = true"
        ") UPDATE swipe_actions SET is_active = false "
        "FROM ranked WHERE swipe_actions.id = ranked.id AND ranked.rn > 1"
    )
    op.execute(
        "WITH ranked AS ("
        " SELECT id, row_number() OVER ("
        "  PARTITION BY user_id, project_id ORDER BY matched_at DESC, id DESC"
        " ) AS rn FROM matches WHERE is_unmatched = false"
        ") UPDATE matches SET is_unmatched = true, unmatched_at = now() "
        "FROM ranked WHERE matches.id = ranked.id AND ranked.rn > 1"
    )
    op.execute(
        "WITH ranked AS ("
        " SELECT id, row_number() OVER ("
        "  PARTITION BY swiper_id, target_type, target_id, context_key "
        "  ORDER BY queued_at DESC, id DESC"
        " ) AS rn FROM swipe_queue_items WHERE is_active = true"
        ") UPDATE swipe_queue_items SET is_active = false, resolution = 'MIGRATED_DUPLICATE', resolved_at = now() "
        "FROM ranked WHERE swipe_queue_items.id = ranked.id AND ranked.rn > 1"
    )

    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_active_swipe_context "
        "ON swipe_actions (swiper_id, target_type, target_id, context_key) WHERE is_active = true"
    )
    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_active_user_project_match "
        "ON matches (user_id, project_id) WHERE is_unmatched = false"
    )
    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_active_queue_target "
        "ON swipe_queue_items (swiper_id, target_type, target_id, context_key) WHERE is_active = true"
    )
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_block ON user_blocks (blocker_id, blocked_id)")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_project_member ON project_members (project_id, user_id)")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_role_name_ci ON user_roles (user_id, lower(role_name))")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_skill_name_ci ON user_skills (user_id, lower(skill_name))")
    op.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_interest_name_ci ON user_interests (user_id, lower(interest_name))")
    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_anonymous_feedback "
        "ON anonymous_feedbacks (project_id, author_id, target_id)"
    )
    op.execute(
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_project_evaluation "
        "ON project_evaluations (project_id, evaluator_id, evaluatee_id)"
    )

    op.execute(
        "DO $$ BEGIN ALTER TABLE projects ADD CONSTRAINT ck_project_date_range "
        "CHECK (end_date > start_date) NOT VALID; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )
    op.execute(
        "DO $$ BEGIN ALTER TABLE projects ADD CONSTRAINT ck_project_member_limit "
        "CHECK (max_members >= 2 AND max_members <= 50) NOT VALID; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )
    op.execute(
        "DO $$ BEGIN ALTER TABLE tasks ADD CONSTRAINT ck_task_date_range "
        "CHECK (deadline > start_date) NOT VALID; "
        "EXCEPTION WHEN duplicate_object THEN NULL; END $$"
    )


def downgrade() -> None:
    Base.metadata.drop_all(bind=op.get_bind())
