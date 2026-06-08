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
