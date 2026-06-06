from uuid import UUID
import re
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, delete
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.profile import (
    UserProfile, UserRole, UserSkill, UserInterest, UserCompletedCourse,
)
from app.models.catalog import SkillCatalog, UserRoleSkill
from app.schemas.profile import (
    ProfileCreate, ProfileUpdate, ProfileResponse, UserCardResponse,
    SkillCreate, RoleCreate, InterestCreate, SkillResponse, RoleResponse,
    InterestResponse, CompletedCourseCreate, CompletedCourseResponse,
)
#from app.services.embedding_service import upsert_user_vector

router = APIRouter(prefix="/api/v1/profiles", tags=["profiles"])


def _profile_embedding_options():
    return (
        selectinload(UserProfile.user).selectinload(User.roles),
        selectinload(UserProfile.user).selectinload(User.skills),
        selectinload(UserProfile.user).selectinload(User.interests),
    )


async def _role_responses(db: AsyncSession, user_id: UUID) -> list[RoleResponse]:
    roles = (
        await db.execute(
            select(UserRole)
            .where(UserRole.user_id == user_id)
            .order_by(UserRole.ordering, UserRole.role_name)
        )
    ).scalars().all()
    responses: list[RoleResponse] = []
    for role in roles:
        rows = (
            await db.execute(
                select(UserRoleSkill, SkillCatalog)
                .join(SkillCatalog, SkillCatalog.id == UserRoleSkill.skill_id)
                .where(UserRoleSkill.user_role_id == role.id)
                .order_by(SkillCatalog.name)
            )
        ).all()
        responses.append(
            RoleResponse(
                id=role.id,
                role_name=role.role_name,
                ordering=role.ordering,
                skills=[
                    SkillResponse(
                        id=assignment.id,
                        skill_name=skill.name,
                        level=assignment.level,
                        needs_improvement=assignment.needs_improvement,
                    )
                    for assignment, skill in rows
                ],
            )
        )
    return responses


async def _get_profile_for_embedding(db: AsyncSession, user_id: UUID) -> UserProfile | None:
    profile_result = await db.execute(
        select(UserProfile)
        .options(*_profile_embedding_options())
        .where(UserProfile.user_id == user_id)
        .execution_options(populate_existing=True)
    )
    return profile_result.scalar_one_or_none()


@router.get("/me", response_model=ProfileResponse)
async def get_my_profile(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    profile_result = await db.execute(
        select(UserProfile).where(UserProfile.user_id == current_user.id)
    )
    profile = profile_result.scalar_one_or_none()
    if not profile:
        profile = UserProfile(user_id=current_user.id, display_name=current_user.full_name)
        db.add(profile)
        await db.flush()

    skills_result = await db.execute(select(UserSkill).where(UserSkill.user_id == current_user.id))
    interests_result = await db.execute(select(UserInterest).where(UserInterest.user_id == current_user.id))

    return ProfileResponse(
        id=profile.id,
        user_id=profile.user_id,
        display_name=profile.display_name,
        bio=profile.bio,
        location=profile.location,
        portfolio_url=profile.portfolio_url,
        github_url=profile.github_url,
        facebook_url=profile.facebook_url,
        short_term_goal=profile.short_term_goal,
        mode=profile.mode,
        is_hidden=profile.is_hidden,
        reputation_score=profile.reputation_score,
        projects_completed=profile.projects_completed,
        avatar_url=current_user.avatar_url,
        email=current_user.email,
        verified_student=current_user.verified_student,
        university=current_user.university,
        roles=await _role_responses(db, current_user.id),
        skills=[SkillResponse.model_validate(s) for s in skills_result.scalars().all()],
        interests=[InterestResponse.model_validate(i) for i in interests_result.scalars().all()],
        created_at=profile.created_at,
    )


@router.put("/me", response_model=ProfileResponse)
async def update_my_profile(
    data: ProfileUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    profile = await _get_profile_for_embedding(db, current_user.id)
    if not profile:
        profile = UserProfile(user_id=current_user.id, display_name=current_user.full_name)
        db.add(profile)
        await db.flush()
        profile = await _get_profile_for_embedding(db, current_user.id)
        if not profile:
            raise HTTPException(status_code=500, detail="Failed to create profile")

    scalar_fields = {
        "display_name", "bio", "location", "portfolio_url", "github_url",
        "facebook_url", "short_term_goal", "mode",
    }
    values = data.model_dump(exclude_unset=True)
    if values.get("mode") == "PROJECT_OWNER":
        values["mode"] = "OWNER"
    for field, value in values.items():
        if field not in scalar_fields:
            continue
        setattr(profile, field, value)

    if data.roles is not None:
        if len(data.roles) > 3:
            raise HTTPException(status_code=400, detail="Maximum 3 roles allowed")
        role_names = [role.role_name.strip().lower() for role in data.roles]
        if len(role_names) != len(set(role_names)):
            raise HTTPException(status_code=400, detail="Roles must be unique")

        existing_roles = (
            await db.execute(select(UserRole).where(UserRole.user_id == current_user.id))
        ).scalars().all()
        for role in existing_roles:
            await db.delete(role)
        await db.flush()
        await db.execute(delete(UserSkill).where(UserSkill.user_id == current_user.id))

        flattened_skills: dict[str, tuple[str, bool]] = {}
        for role_input in sorted(data.roles, key=lambda item: item.ordering):
            role = UserRole(
                user_id=current_user.id,
                role_name=role_input.role_name.strip(),
                ordering=role_input.ordering,
            )
            db.add(role)
            await db.flush()
            seen_skills: set[str] = set()
            for skill_input in role_input.skills:
                normalized = skill_input.skill_name.strip().lower()
                if normalized in seen_skills:
                    continue
                seen_skills.add(normalized)
                catalog = (
                    await db.execute(
                        select(SkillCatalog).where(func.lower(SkillCatalog.name) == normalized)
                    )
                ).scalar_one_or_none()
                if not catalog:
                    catalog = SkillCatalog(
                        slug=re.sub(r"[^a-z0-9]+", "-", normalized).strip("-"),
                        name=skill_input.skill_name.strip(),
                    )
                    db.add(catalog)
                    await db.flush()
                db.add(
                    UserRoleSkill(
                        user_role_id=role.id,
                        skill_id=catalog.id,
                        level=skill_input.level,
                        needs_improvement=skill_input.needs_improvement,
                    )
                )
                flattened_skills.setdefault(
                    normalized, (skill_input.level, skill_input.needs_improvement)
                )
        for skill_name, (level, needs_improvement) in flattened_skills.items():
            db.add(
                UserSkill(
                    user_id=current_user.id,
                    skill_name=skill_name,
                    level=level,
                    needs_improvement=needs_improvement,
                )
            )

    if data.interests is not None:
        normalized_interests = {
            item.strip().lower(): item.strip()
            for item in data.interests
            if item.strip()
        }
        await db.execute(delete(UserInterest).where(UserInterest.user_id == current_user.id))
        for interest in normalized_interests.values():
            db.add(UserInterest(user_id=current_user.id, interest_name=interest))

    await db.flush()
    embedding_id = None
    #embedding_id = upsert_user_vector(profile)
    if embedding_id:
        profile.embedding_id = embedding_id

    return await get_my_profile(current_user, db)


@router.post("/me/hide")
async def hide_profile(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    profile = await _get_profile_for_embedding(db, current_user.id)
    if profile:
        profile.is_hidden = True
        #upsert_user_vector(profile)
        pass
    return {"message": "Profile hidden from swipe pool"}


@router.post("/me/unhide")
async def unhide_profile(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    profile = await _get_profile_for_embedding(db, current_user.id)
    if profile:
        profile.is_hidden = False
        #upsert_user_vector(profile)
        pass
    return {"message": "Profile visible in swipe pool"}


@router.post("/me/skills", response_model=SkillResponse, status_code=201)
async def add_skill(
    data: SkillCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    existing_result = await db.execute(
        select(UserSkill).where(
            UserSkill.user_id == current_user.id,
            func.lower(UserSkill.skill_name) == data.skill_name.strip().lower(),
        )
    )
    existing_skill = existing_result.scalar_one_or_none()
    if existing_skill:
        return SkillResponse.model_validate(existing_skill)

    skill = UserSkill(
        user_id=current_user.id,
        skill_name=data.skill_name.strip(),
        level=data.level,
        needs_improvement=data.needs_improvement
    )
    db.add(skill)
    await db.flush()

    profile = await _get_profile_for_embedding(db, current_user.id)
    if profile:
        pass
        #upsert_user_vector(profile)

    return SkillResponse.model_validate(skill)


@router.delete("/me/skills/{skill_id}")
async def remove_skill(
    skill_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    skill = await db.get(UserSkill, skill_id)
    if skill and skill.user_id == current_user.id:
        await db.delete(skill)

        profile = await _get_profile_for_embedding(db, current_user.id)
        if profile:
            pass
            #upsert_user_vector(profile)

    return {"message": "Skill removed"}

@router.post("/me/roles", response_model=RoleResponse, status_code=201)
async def add_role(
    data: RoleCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    existing_result = await db.execute(
        select(UserRole).where(
            UserRole.user_id == current_user.id,
            func.lower(UserRole.role_name) == data.role_name.strip().lower(),
        )
    )
    existing_role = existing_result.scalar_one_or_none()
    if existing_role:
        return RoleResponse.model_validate(existing_role)

    count_result = await db.execute(
        select(func.count()).select_from(UserRole).where(UserRole.user_id == current_user.id)
    )
    if count_result.scalar() >= 3:
        raise HTTPException(status_code=400, detail="Maximum 3 roles allowed")

    role = UserRole(
        user_id=current_user.id,
        role_name=data.role_name.strip(),
        ordering=data.ordering
    )
    db.add(role)
    await db.flush()

    profile = await _get_profile_for_embedding(db, current_user.id)
    if profile:
        pass
        #upsert_user_vector(profile)

    return RoleResponse.model_validate(role)



@router.delete("/me/roles/{role_id}")
async def remove_role(
    role_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    role = await db.get(UserRole, role_id)
    if role and role.user_id == current_user.id:
        await db.delete(role)

        profile = await _get_profile_for_embedding(db, current_user.id)
        if profile:
            pass
            #upsert_user_vector(profile)

    return {"message": "Role removed"}


# ... existing code ...

@router.post("/me/interests", response_model=InterestResponse, status_code=201)
async def add_interest(
    data: InterestCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    existing_result = await db.execute(
        select(UserInterest).where(
            UserInterest.user_id == current_user.id,
            func.lower(UserInterest.interest_name) == data.interest_name.strip().lower(),
        )
    )
    existing_interest = existing_result.scalar_one_or_none()
    if existing_interest:
        return InterestResponse.model_validate(existing_interest)

    interest = UserInterest(
        user_id=current_user.id,
        interest_name=data.interest_name.strip()
    )
    db.add(interest)
    await db.flush()

    profile = await _get_profile_for_embedding(db, current_user.id)
    if profile:
        pass
        #upsert_user_vector(profile)

    return InterestResponse.model_validate(interest)

# ... existing code ...


@router.delete("/me/interests/{interest_id}")
async def remove_interest(
    interest_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    interest = await db.get(UserInterest, interest_id)
    if interest and interest.user_id == current_user.id:
        await db.delete(interest)
    return {"message": "Interest removed"}


@router.post("/me/completed-courses", response_model=CompletedCourseResponse, status_code=201)
async def add_completed_course(
    data: CompletedCourseCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    course = UserCompletedCourse(
        user_id=current_user.id,
        course_title=data.course_title,
        source=data.source,
        course_id=data.course_id,
    )
    db.add(course)
    await db.flush()
    return CompletedCourseResponse.model_validate(course)


@router.get("/{user_id}", response_model=UserCardResponse)
async def get_user_profile(
    user_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    profile_result = await db.execute(
        select(UserProfile).where(UserProfile.user_id == user_id)
    )
    profile = profile_result.scalar_one_or_none()
    if not profile:
        raise HTTPException(status_code=404, detail="Profile not found")

    target_user = await db.get(User, user_id)
    skills_result = await db.execute(select(UserSkill).where(UserSkill.user_id == user_id))

    return UserCardResponse(
        user_id=profile.user_id,
        display_name=profile.display_name,
        avatar_url=target_user.avatar_url if target_user else None,
        verified_student=target_user.verified_student if target_user else False,
        university=target_user.university if target_user else None,
        bio=profile.bio,
        roles=await _role_responses(db, user_id),
        skills=[SkillResponse.model_validate(s) for s in skills_result.scalars().all()],
        location=profile.location,
        github_url=profile.github_url,
        reputation_score=profile.reputation_score,
        projects_completed=profile.projects_completed,
        match_score=0.0,
    )
