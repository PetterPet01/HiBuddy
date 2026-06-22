from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, or_

from app.database import get_db
from app.models.user import User
from app.models.profile import UserProfile, UserRole, UserSkill
from app.models.trust_safety import UserBlock
from app.core.dependencies import get_current_user
from app.services.fuzzy_match import normalize_skill, best_fuzzy_score

router = APIRouter(prefix="/api/v1/search", tags=["search"])


def _alias_terms(value: str) -> list[str]:
    """Original term plus its canonical alias, for broader DB matching."""
    terms = {value.strip().lower(), normalize_skill(value)}
    return [t for t in terms if t]


@router.get("/users")
async def search_users(
    q: str | None = Query(None, description="Search by name, bio, or university"),
    skill: str | None = Query(None, description="Filter by skill name"),
    role: str | None = Query(None, description="Filter by role name"),
    limit: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    # Coarse candidate pool: widen the pool with alias-expanded ilike terms so
    # "js" can still surface "JavaScript". Final ranking is fuzzy (below).
    user_ids = set()

    if skill:
        skill_conditions = [UserSkill.skill_name.ilike(f"%{term}%") for term in _alias_terms(skill)]
        skill_query = select(UserSkill.user_id).where(or_(*skill_conditions)).limit(limit * 5)
        result = await db.execute(skill_query)
        user_ids.update(r[0] for r in result.all())

    if role:
        role_conditions = [UserRole.role_name.ilike(f"%{term}%") for term in _alias_terms(role)]
        role_query = select(UserRole.user_id).where(or_(*role_conditions)).limit(limit * 5)
        result = await db.execute(role_query)
        role_ids = {r[0] for r in result.all()}
        if user_ids:
            user_ids &= role_ids
        else:
            user_ids = role_ids

    if (skill or role) and not user_ids:
        return []

    conditions = [
        User.is_active == True,
        User.email_verified == True,
        User.id != current_user.id,
    ]

    if q:
        search_term = f"%{q}%"
        conditions.append(or_(
            User.full_name.ilike(search_term),
            User.university.ilike(search_term),
        ))

    if user_ids:
        conditions.append(User.id.in_(user_ids))

    blocked_rows = await db.execute(
        select(UserBlock.blocker_id, UserBlock.blocked_id).where(
            or_(
                UserBlock.blocker_id == current_user.id,
                UserBlock.blocked_id == current_user.id,
            )
        )
    )
    blocked_ids = {
        blocked_id if blocker_id == current_user.id else blocker_id
        for blocker_id, blocked_id in blocked_rows.all()
    }
    if blocked_ids:
        conditions.append(~User.id.in_(blocked_ids))

    # Pull a larger window so fuzzy ranking can reorder before truncation.
    query = select(User).where(*conditions).limit(limit * 5 if (skill or role) else limit)
    result = await db.execute(query)
    users = result.scalars().all()

    scored = []
    for user in users:
        profile_result = await db.execute(
            select(UserProfile).where(UserProfile.user_id == user.id)
        )
        profile = profile_result.scalar_one_or_none()

        roles_result = await db.execute(
            select(UserRole).where(UserRole.user_id == user.id).order_by(UserRole.ordering).limit(3)
        )
        skills_result = await db.execute(
            select(UserSkill).where(UserSkill.user_id == user.id)
        )
        user_roles = list(roles_result.scalars())
        user_skills = list(skills_result.scalars())

        # Relevance: best fuzzy match against the requested skill/role.
        relevance = 0.0
        if skill:
            relevance += best_fuzzy_score(skill, [s.skill_name for s in user_skills])
        if role:
            relevance += best_fuzzy_score(role, [r.role_name for r in user_roles])

        scored.append((
            relevance,
            {
                "user_id": str(user.id),
                "display_name": profile.display_name if profile else user.full_name,
                "avatar_url": user.avatar_url,
                "verified_student": user.verified_student,
                "university": user.university,
                "bio": profile.bio if profile else None,
                "roles": [{"role_name": r.role_name} for r in user_roles],
                "skills": [{"skill_name": s.skill_name, "level": s.level} for s in user_skills],
                "reputation_score": profile.reputation_score if profile else 3.0,
            },
        ))

    # Sort by relevance when a skill/role filter is present; otherwise keep DB order.
    if skill or role:
        scored.sort(key=lambda item: item[0], reverse=True)

    return [card for _, card in scored[:limit]]

