"""Deterministic, explainable matching for project role slots."""

import re
from datetime import datetime, timezone

from app.models.project import Project, ProjectRoleSlot
from app.models.user import User


ROLE_ALIASES = {
    "ai ml engineer": "ai engineer",
    "machine learning engineer": "ai engineer",
    "android developer": "mobile developer",
    "ios developer": "mobile developer",
    "full stack developer": "fullstack developer",
    "fullstack engineer": "fullstack developer",
    "front end developer": "frontend developer",
    "back end developer": "backend developer",
    "ui ux designer": "ui ux designer",
}
LEVEL_VALUE = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3}


def normalize_name(value: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", " ", (value or "").lower()).strip()
    return ROLE_ALIASES.get(normalized, normalized)


def _jaccard(left: set[str], right: set[str]) -> float:
    if not left and not right:
        return 0.5
    union = left | right
    return len(left & right) / len(union) if union else 0.0


def _user_role_skills(user: User, role_name: str) -> dict[str, str]:
    normalized_role = normalize_name(role_name)
    scoped: dict[str, str] = {}
    for role in getattr(user, "roles", []):
        if normalize_name(role.role_name) != normalized_role:
            continue
        for assignment in getattr(role, "role_skills", []):
            skill = getattr(assignment, "skill", None)
            if skill:
                scoped[normalize_name(skill.name)] = assignment.level
    if scoped:
        return scoped
    return {
        normalize_name(skill.skill_name): skill.level
        for skill in getattr(user, "skills", [])
    }


def _slot_requirements(slot: ProjectRoleSlot) -> dict[str, tuple[str, bool]]:
    rows = getattr(slot, "skill_requirements_rows", [])
    structured = {
        normalize_name(row.skill.name): (row.minimum_level, row.is_required)
        for row in rows
        if getattr(row, "skill", None)
    }
    if structured:
        return structured
    legacy = slot.skill_requirements or {}
    return {
        normalize_name(name): (str(level or "BEGINNER").upper(), True)
        for name, level in legacy.items()
        if normalize_name(name) != "requirements"
    }


def _slot_score(user: User, slot: ProjectRoleSlot) -> tuple[float, dict]:
    user_roles = {normalize_name(role.role_name) for role in getattr(user, "roles", [])}
    slot_role = normalize_name(slot.role_name)
    role_score = 100.0 if slot_role in user_roles else 0.0
    user_skills = _user_role_skills(user, slot.role_name)
    requirements = _slot_requirements(slot)

    if not requirements:
        skill_score = 60.0
        matched_skills: list[str] = []
        missing_skills: list[str] = []
    else:
        earned = 0.0
        total = 0.0
        matched_skills = []
        missing_skills = []
        for skill_name, (minimum_level, required) in requirements.items():
            weight = 2.0 if required else 1.0
            total += weight
            actual = LEVEL_VALUE.get(user_skills.get(skill_name, ""), 0)
            required_level = LEVEL_VALUE.get(minimum_level.upper(), 1)
            if actual >= required_level:
                earned += weight
                matched_skills.append(skill_name)
            else:
                missing_skills.append(skill_name)
        skill_score = earned / total * 100 if total else 60.0

    availability_score = 100.0 if slot.filled < slot.count else 0.0
    score = role_score * 0.55 + skill_score * 0.35 + availability_score * 0.10
    return score, {
        "role": slot.role_name,
        "role_fit": round(role_score, 1),
        "skill_fit": round(skill_score, 1),
        "slot_available": slot.filled < slot.count,
        "matched_skills": matched_skills,
        "missing_skills": missing_skills,
    }


def best_role_slot(user: User, project: Project) -> tuple[ProjectRoleSlot | None, float, dict]:
    open_slots = [slot for slot in project.role_slots if slot.filled < slot.count]
    if not open_slots:
        return None, 0.0, {"reason": "No open role slots"}
    ranked = [(slot, *_slot_score(user, slot)) for slot in open_slots]
    slot, score, details = max(ranked, key=lambda item: (item[1], str(item[0].id)))
    return slot, score, details


def calculate_project_score_details(
    user: User, project: Project, owner: User | None = None
) -> tuple[float, dict, ProjectRoleSlot | None]:
    slot, slot_fit, slot_details = best_role_slot(user, project)
    interests = {normalize_name(item.interest_name) for item in getattr(user, "interests", [])}
    project_terms = {
        normalize_name(project.field),
        *re.findall(r"[a-z0-9]+", (project.description or "").lower()),
    }
    interest_score = _jaccard(interests, project_terms) * 100 if interests else 50.0
    profile = getattr(user, "profile", None)
    mode = (profile.mode if profile else "BOTH").upper()
    commitment = {
        "CONTRIBUTOR": {"CASUAL": 100, "MODERATE": 80, "INTENSIVE": 55},
        "OWNER": {"CASUAL": 50, "MODERATE": 80, "INTENSIVE": 100},
        "BOTH": {"CASUAL": 80, "MODERATE": 100, "INTENSIVE": 80},
    }.get(mode, {}).get((project.commitment_level or "MODERATE").upper(), 60)
    owner_reputation = (
        float(owner.profile.reputation_score)
        if owner and owner.profile
        else 3.0
    )
    quality = min(owner_reputation / 5.0, 1.0) * 100
    age_days = _days_ago(project.created_at)
    recency = 100 if age_days <= 7 else 70 if age_days <= 14 else 40 if age_days <= 30 else 10
    factors = {
        "role_and_skills": round(slot_fit, 1),
        "interest": round(interest_score, 1),
        "commitment": float(commitment),
        "owner_quality": round(quality, 1),
        "recency": float(recency),
    }
    score = (
        factors["role_and_skills"] * 0.65
        + factors["interest"] * 0.10
        + factors["commitment"] * 0.10
        + factors["owner_quality"] * 0.10
        + factors["recency"] * 0.05
    )
    explanation = {
        "matched_role": slot.role_name if slot else None,
        "factors": factors,
        "slot": slot_details,
    }
    return round(min(score, 100.0), 1), explanation, slot


def calculate_user_score_details(
    owner: User,
    target_user: User,
    project: Project,
) -> tuple[float, dict, ProjectRoleSlot | None]:
    project_score, explanation, slot = calculate_project_score_details(
        target_user, project, owner
    )
    profile = target_user.profile
    reputation = min((profile.reputation_score if profile else 3.0) / 5.0, 1.0) * 100
    experience = min((profile.projects_completed if profile else 0) / 10.0, 1.0) * 100
    verified = 100.0 if target_user.verified_student else 0.0
    score = project_score * 0.75 + reputation * 0.12 + experience * 0.08 + verified * 0.05
    explanation["factors"].update(
        {
            "reputation": round(reputation, 1),
            "experience": round(experience, 1),
            "verified_student": verified,
        }
    )
    return round(min(score, 100.0), 1), explanation, slot


def calculate_project_score(user: User, project: Project, owner: User | None = None) -> float:
    return calculate_project_score_details(user, project, owner)[0]


def calculate_user_score(owner: User, target_user: User, target_profile, owner_projects: list[Project]) -> float:
    recruiting = [project for project in owner_projects if project.status == "RECRUITING"]
    if not recruiting:
        return 0.0
    return max(
        calculate_user_score_details(owner, target_user, project)[0]
        for project in recruiting
    )


def calculate_match_score_for_pair(
    user: User, project: Project, owner: User | None = None
) -> float:
    return calculate_project_score_details(user, project, owner)[0]


def _days_ago(value) -> float:
    if value is None:
        return 999.0
    if value.tzinfo is None:
        value = value.replace(tzinfo=timezone.utc)
    return (datetime.now(timezone.utc) - value).total_seconds() / 86400.0
