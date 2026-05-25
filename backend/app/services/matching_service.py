"""
Multi-factor matching & scoring engine for HiBuddy.

Two modes:
  - CONTRIBUTOR: score each Project for a given User (browsing projects to join)
  - OWNER:       score each User for a given Owner's recruiting projects

Each mode uses weighted factors (role match is the dominant signal at 40 %).
Final score is a weighted sum capped at 100.
"""

import logging
from datetime import datetime, timezone, timedelta

from app.models.user import User
from app.models.profile import UserProfile
from app.models.project import Project

logger = logging.getLogger(__name__)

# ── helpers ──────────────────────────────────────────────────────────

def _jaccard(set1: set[str], set2: set[str]) -> float:
    """Jaccard similarity 0-1. Returns 0.5 when both sets are empty (neutral)."""
    if not set1 and not set2:
        return 0.5
    union = set1 | set2
    if not union:
        return 0.0
    return len(set1 & set2) / len(union)


def _norm(value: float, max_val: float) -> float:
    """Normalise *value* to 0-100 against *max_val*."""
    if max_val <= 0:
        return 0.0
    return min(value / max_val, 1.0) * 100.0


# ── CONTRIBUTOR mode: project → user ─────────────────────────────────

def calculate_project_score(user: User, project: Project,
                            owner: User | None = None) -> float:
    """
    Score a project for a contributor browsing projects.

    Weights (total 100):
      Role fit               40 %
      Skill match            25 %
      Interest / field       10 %
      Commitment compatibility  8 %
      Work-mode match         5 %
      Quality signals         7 %
      Recency boost           5 %
    """
    weights = {
        "role_fit":             40,
        "skill_match":          25,
        "interest_field":       10,
        "commitment":            8,
        "work_mode":             5,
        "quality":               7,
        "recency":               5,
    }

    scores: dict[str, float] = {}

    scores["role_fit"] = _role_fit_score(user, project)
    scores["skill_match"] = _skill_match_score_project(user, project)
    scores["interest_field"] = _interest_field_score(user, project)
    scores["commitment"] = _commitment_score(user, project)
    scores["work_mode"] = _work_mode_score(user, project)
    scores["quality"] = _quality_signal_score(project, owner)
    scores["recency"] = _recency_score(project)

    total = sum(scores[k] * weights[k] / 100.0 for k in weights)
    return round(min(total, 100.0), 1)


# ── OWNER mode: user → owner's projects ──────────────────────────────

def calculate_user_score(owner: User, target_user: User,
                         target_profile: UserProfile,
                         owner_projects: list[Project]) -> float:
    """
    Score a contributor for an owner browsing people.

    Weights (total 100):
      Role compatibility     40 %
      Skill match            25 %
      Reputation             10 %
      Experience              8 %
      Interest alignment      7 %
      Location                5 %
      Verified student        5 %
    """
    weights = {
        "role_compat":          40,
        "skill_match":          25,
        "reputation":           10,
        "experience":            8,
        "interest_alignment":    7,
        "location":              5,
        "verified_student":      5,
    }

    rec = [p for p in owner_projects if p.status == "RECRUITING"]

    scores: dict[str, float] = {}

    # 1. Role compatibility (40 %)
    scores["role_compat"] = _owner_role_compat_score(target_user, rec)

    # 2. Skill match (25 %)
    scores["skill_match"] = _owner_skill_match_score(target_user, rec)

    # 3. Reputation (10 %)
    scores["reputation"] = _norm(target_profile.reputation_score, 5.0)

    # 4. Experience (8 %)
    scores["experience"] = _norm(target_profile.projects_completed, 10.0)

    # 5. Interest alignment (7 %)
    scores["interest_alignment"] = _owner_interest_score(target_user, rec)

    # 6. Location (5 %)
    scores["location"] = _owner_location_score(owner, target_profile)

    # 7. Verified student (5 %)
    scores["verified_student"] = 100.0 if target_user.verified_student else 0.0

    total = sum(scores[k] * weights[k] / 100.0 for k in weights)
    return round(min(total, 100.0), 1)


# ── Combined score when a match is created ────────────────────────────

def calculate_match_score_for_pair(user: User, project: Project) -> float:
    """Average of both perspectives when a mutual like occurs."""
    proj_score = calculate_project_score(user, project)
    user_score = _calculate_user_score_single_project(user, project)
    return round((proj_score + user_score) / 2.0, 1)


# ── Factor implementations: CONTRIBUTOR mode ─────────────────────────

def _role_fit_score(user: User, project: Project) -> float:
    """40 % — user role vs unfilled project role slots."""
    user_roles = {r.role_name.lower() for r in user.roles}
    if not user_roles:
        return 50.0

    has_unfilled_match = False
    has_any_match = False

    for slot in project.role_slots:
        slot_name = slot.role_name.lower()
        if slot_name in user_roles:
            has_any_match = True
            if slot.filled < slot.count:
                has_unfilled_match = True
                break

    if has_unfilled_match:
        return 100.0
    if has_any_match:
        return 40.0
    return 0.0


def _skill_match_score_project(user: User, project: Project) -> float:
    """25 % — Jaccard between user skills and project role-slot skill requirements."""
    user_skills = {s.skill_name.lower() for s in user.skills}

    required: set[str] = set()
    for slot in project.role_slots:
        if slot.skill_requirements:
            required.update(k.lower() for k in slot.skill_requirements)

    return _jaccard(user_skills, required) * 100.0


def _interest_field_score(user: User, project: Project) -> float:
    """10 % — Jaccard(user interests ↔ project field + description words)."""
    interests = {i.interest_name.lower() for i in user.interests}
    project_words = _tokenise(f"{project.field} {project.description or ''}")
    if not interests:
        return 50.0
    return _jaccard(interests, project_words) * 100.0


def _commitment_score(user: User, project: Project) -> float:
    """8 % — match user mode preference against project commitment level."""
    profile = getattr(user, "profile", None)
    user_mode = (profile.mode if profile else "BOTH").upper()
    proj_level = (project.commitment_level or "MODERATE").upper()

    # mapping  user_mode  →  preferred commitment
    preference_map = {
        "CONTRIBUTOR": {"CASUAL": 100, "MODERATE": 70, "INTENSIVE": 40},
        "OWNER":       {"INTENSIVE": 100, "MODERATE": 70, "CASUAL": 40},
        "BOTH":        {"CASUAL": 80, "MODERATE": 90, "INTENSIVE": 80},
    }
    return float(preference_map.get(user_mode, {}).get(proj_level, 50))


def _work_mode_score(user: User, project: Project) -> float:
    """5 % — work-mode match."""
    profile = getattr(user, "profile", None)
    has_location = bool(profile and profile.location)
    if not has_location:
        return 60.0  # neutral

    pw = (project.work_mode or "").upper()
    # simplified: ONLINE <-> OFFLINE conflict, HYBRID bridges
    match_map = {
        ("ONLINE", "ONLINE"):   100,
        ("OFFLINE", "OFFLINE"): 100,
        ("HYBRID", "HYBRID"):   100,
        ("HYBRID", "ONLINE"):   60,
        ("ONLINE", "HYBRID"):   60,
        ("HYBRID", "OFFLINE"):  60,
        ("OFFLINE", "HYBRID"):  60,
    }
    # user doesn't declare a work mode, infer from location presence
    return float(match_map.get(("HYBRID", pw), 30))


def _quality_signal_score(project: Project,
                         owner: User | None = None) -> float:
    """7 % — owner reputation + slot fill rate."""
    owner_reputation = 3.0
    if owner and owner.profile:
        owner_reputation = float(owner.profile.reputation_score)
    elif hasattr(project, "owner") and project.owner:
        owner_reputation = float(project.owner.profile.reputation_score) if project.owner.profile else 3.0
    rep_score = _norm(owner_reputation, 5.0)  # 0-100

    total_count = sum(s.count for s in project.role_slots)
    total_filled = sum(s.filled for s in project.role_slots)
    fill_score = (total_filled / total_count * 100.0) if total_count > 0 else 0.0

    return rep_score * 0.6 + fill_score * 0.4   # 60 % rep, 40 % fill rate


def _recency_score(project: Project) -> float:
    """5 % — newer projects get a small boost."""
    age = _days_ago(getattr(project, "created_at", None))
    if age <= 7:
        return 100.0
    if age <= 14:
        return 70.0
    if age <= 30:
        return 40.0
    return 10.0


# ── Factor implementations: OWNER mode ───────────────────────────────

def _owner_role_compat_score(target_user: User,
                             owner_projects: list[Project]) -> float:
    """40 % — target user's roles vs unfilled slots across all owner's recruiting projects."""
    user_roles = {r.role_name.lower() for r in target_user.roles}
    if not user_roles:
        return 50.0

    for proj in owner_projects:
        if proj.status != "RECRUITING":
            continue
        for slot in proj.role_slots:
            if slot.role_name.lower() in user_roles and slot.filled < slot.count:
                return 100.0

    for proj in owner_projects:
        if proj.status != "RECRUITING":
            continue
        for slot in proj.role_slots:
            if slot.role_name.lower() in user_roles:
                return 40.0

    return 0.0


def _owner_skill_match_score(target_user: User,
                             owner_projects: list[Project]) -> float:
    """25 % — Jaccard(target skills ↔ all required skills from owner's recruiting projects)."""
    target_skills = {s.skill_name.lower() for s in target_user.skills}

    required: set[str] = set()
    for proj in owner_projects:
        if proj.status != "RECRUITING":
            continue
        for slot in proj.role_slots:
            if slot.skill_requirements:
                required.update(k.lower() for k in slot.skill_requirements)

    return _jaccard(target_skills, required) * 100.0


def _owner_interest_score(target_user: User,
                          owner_projects: list[Project]) -> float:
    """7 % — Jaccard(target interests ↔ project fields of owner's recruiting projects)."""
    interests = {i.interest_name.lower() for i in target_user.interests}
    project_fields: set[str] = set()
    for proj in owner_projects:
        if proj.status == "RECRUITING" and proj.field:
            project_fields.update(_tokenise(proj.field))
    if not interests:
        return 50.0
    return _jaccard(interests, project_fields) * 100.0


def _owner_location_score(owner: User, target_profile: UserProfile) -> float:
    """5 % — same location (case-insensitive)."""
    owner_location = (owner.profile.location or "").strip().lower() if owner.profile else ""
    target_location = (target_profile.location or "").strip().lower()
    if owner_location and target_location and owner_location == target_location:
        return 100.0
    return 0.0


# ── Single-project version (for match creation) ──────────────────────

def _calculate_user_score_single_project(user: User,
                                         project: Project) -> float:
    """Owner-mode scoring limited to one project."""
    weights = {
        "role_compat":          40,
        "skill_match":          25,
        "reputation":           10,
        "experience":            8,
        "interest_alignment":    7,
        "location":              5,
        "verified_student":      5,
    }

    profile = user.profile if user.profile else None
    if profile is None:
        return 0.0

    rec = [project] if project.status == "RECRUITING" else []

    scores: dict[str, float] = {}
    scores["role_compat"]       = _owner_role_compat_score(user, rec)
    scores["skill_match"]       = _owner_skill_match_score(user, rec)
    scores["reputation"]        = _norm(profile.reputation_score, 5.0)
    scores["experience"]        = _norm(profile.projects_completed, 10.0)
    scores["interest_alignment"]= _owner_interest_score(user, rec)
    scores["location"]          = _owner_location_score(
        _owner_of(project), profile)
    scores["verified_student"]  = 100.0 if user.verified_student else 0.0

    total = sum(scores[k] * weights[k] / 100.0 for k in weights)
    return round(min(total, 100.0), 1)


# ── tiny utils ───────────────────────────────────────────────────────

def _tokenise(text: str) -> set[str]:
    return set((text or "").lower().split())


def _days_ago(dt) -> float:
    if dt is None:
        return 999
    now = datetime.now(timezone.utc)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return (now - dt).total_seconds() / 86400.0


def _owner_of(project: Project):
    """Return the project's owner User (may trigger lazy load)."""
    return getattr(project, "owner", None) or getattr(project, "_owner", None)
