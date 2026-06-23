from datetime import datetime, timezone
from types import SimpleNamespace
from uuid import uuid4

from app.services.matching_service import (
    calculate_project_score_details,
    normalize_name,
)


def role(name, skills):
    return SimpleNamespace(
        role_name=name,
        role_skills=[
            SimpleNamespace(
                level=level,
                skill=SimpleNamespace(name=skill_name),
            )
            for skill_name, level in skills
        ],
    )


def requirement(name, level, required=True):
    return SimpleNamespace(
        minimum_level=level,
        is_required=required,
        skill=SimpleNamespace(name=name),
    )


def test_role_aliases_are_normalized():
    assert normalize_name("Android Developer") == "mobile developer"
    assert normalize_name("Full-Stack Developer") == "fullstack developer"


def test_matching_uses_skills_from_the_matched_role_only():
    backend = role(
        "Backend Developer",
        [("Python", "ADVANCED"), ("PostgreSQL", "ADVANCED")],
    )
    designer = role("UI/UX Designer", [("Figma", "ADVANCED")])
    user = SimpleNamespace(
        roles=[backend, designer],
        skills=[],
        interests=[],
        profile=SimpleNamespace(mode="CONTRIBUTOR"),
    )
    slot = SimpleNamespace(
        id=uuid4(),
        role_name="Backend Developer",
        count=1,
        filled=0,
        skill_requirements=None,
        skill_requirements_rows=[
            requirement("PostgreSQL", "INTERMEDIATE"),
            requirement("Figma", "INTERMEDIATE"),
        ],
    )
    project = SimpleNamespace(
        role_slots=[slot],
        field="Web",
        description="Backend platform",
        commitment_level="MODERATE",
        created_at=datetime.now(timezone.utc),
    )
    owner = SimpleNamespace(
        profile=SimpleNamespace(reputation_score=4.0),
    )

    score, explanation, matched_slot = calculate_project_score_details(
        user, project, owner
    )

    assert matched_slot is slot
    assert "postgresql" in explanation["slot"]["matched_skills"]
    assert "figma" in explanation["slot"]["missing_skills"]
    assert 0 < score < 100


def test_newbie_bootstrap_reputation_and_experience():
    from datetime import datetime, timedelta, timezone
    from app.services.matching_service import _get_user_reputation_and_experience_with_boost

    # 1. New user with created_at under 30 days
    new_user = SimpleNamespace(
        created_at=datetime.now(timezone.utc) - timedelta(days=5),
        profile=SimpleNamespace(reputation_score=0.0, projects_completed=0),
    )
    rep, exp = _get_user_reputation_and_experience_with_boost(new_user)
    # 3.8 reputation is 76%, 3 completed projects is 30%
    assert rep == 76.0
    assert exp == 30.0

    # 2. Old user but has never completed projects or received rating
    old_inactive_user = SimpleNamespace(
        created_at=datetime.now(timezone.utc) - timedelta(days=40),
        profile=SimpleNamespace(reputation_score=0.0, projects_completed=0),
    )
    rep2, exp2 = _get_user_reputation_and_experience_with_boost(old_inactive_user)
    assert rep2 == 76.0
    assert exp2 == 30.0

    # 3. Old active user with high reputation and projects completed
    old_active_user = SimpleNamespace(
        created_at=datetime.now(timezone.utc) - timedelta(days=40),
        profile=SimpleNamespace(reputation_score=4.5, projects_completed=5),
    )
    rep3, exp3 = _get_user_reputation_and_experience_with_boost(old_active_user)
    # 4.5 reputation is 90%, 5 projects completed is 50%
    assert rep3 == 90.0
    assert exp3 == 50.0


def test_verified_student_influence():
    from datetime import datetime, timedelta, timezone
    from app.services.matching_service import (
        calculate_project_score_details,
        calculate_user_score_details,
    )

    backend = role("Backend Developer", [])
    user = SimpleNamespace(
        roles=[backend],
        skills=[],
        interests=[],
        verified_student=True,
        created_at=datetime.now(timezone.utc) - timedelta(days=40),
        profile=SimpleNamespace(reputation_score=4.0, projects_completed=2),
    )
    slot = SimpleNamespace(
        id=uuid4(),
        role_name="Backend Developer",
        count=1,
        filled=0,
        skill_requirements=None,
        skill_requirements_rows=[],
    )
    project = SimpleNamespace(
        role_slots=[slot],
        field="Web",
        description="Test description",
        created_at=datetime.now(timezone.utc),
    )

    # Owner 1 is verified
    owner_verified = SimpleNamespace(
        verified_student=True,
        created_at=datetime.now(timezone.utc) - timedelta(days=40),
        profile=SimpleNamespace(reputation_score=4.0, projects_completed=5),
    )

    # Owner 2 is not verified
    owner_unverified = SimpleNamespace(
        verified_student=False,
        created_at=datetime.now(timezone.utc) - timedelta(days=40),
        profile=SimpleNamespace(reputation_score=4.0, projects_completed=5),
    )

    # 1. Project score calculation
    _, explanation_v, _ = calculate_project_score_details(user, project, owner_verified)
    _, explanation_uv, _ = calculate_project_score_details(user, project, owner_unverified)

    # Verify commitment is removed
    assert "commitment" not in explanation_v["factors"]
    assert "commitment" not in explanation_uv["factors"]

    # Verify owner_quality: 4.0 reputation = 80%.
    # Owner verified gets +15 bonus -> 95.0. Owner unverified gets 80.0
    assert explanation_v["factors"]["owner_quality"] == 95.0
    assert explanation_uv["factors"]["owner_quality"] == 80.0

    # 2. User score calculation (owner viewing user profile)
    user_score_verified, explanation_user_v, _ = calculate_user_score_details(owner_unverified, user, project)

    # Verify user's verified_student status is factored as 100.0
    assert explanation_user_v["factors"]["verified_student"] == 100.0

