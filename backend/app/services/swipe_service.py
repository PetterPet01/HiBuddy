import logging
from datetime import datetime, timedelta, timezone
from uuid import UUID
from typing import Any

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, func
from sqlalchemy.orm import selectinload
from app.config import get_settings
from app.database import async_session
from app.milvus_client import connect_milvus, disconnect_milvus
from app.models.swipe import SwipeAction, Match, SwipeQueueItem
from app.models.project import Project, ProjectRoleSlot, ProjectMember
from app.models.user import User
from app.models.profile import UserProfile, UserRole, UserSkill
from app.models.chat import Chat, Notification, Message
from app.models.trust_safety import UserBlock
from app.models.operations import OutboxEvent
from app.models.catalog import UserRoleSkill, ProjectRoleSkillRequirement
from app.services.embedding_service import encode_text
from app.services.presence_service import presence_manager
from app.services.matching_service import (
    calculate_project_score,
    calculate_user_score,
    calculate_match_score_for_pair,
    calculate_project_score_details,
    calculate_user_score_details,
)

logger = logging.getLogger(__name__)
settings = get_settings()
QUEUE_LIMIT_PER_TYPE = 3
QUEUE_TTL = timedelta(hours=24)


async def get_daily_likes_remaining(db: AsyncSession, user_id: UUID) -> int:
    today = datetime.now(timezone.utc).date()
    count = await db.execute(
        select(func.count()).select_from(SwipeAction).where(
            SwipeAction.swiper_id == user_id,
            func.date(SwipeAction.created_at) == today,
            SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
        )
    )
    return max(0, settings.SWIPE_DAILY_LIKE_LIMIT - count.scalar())

async def get_daily_superlikes_remaining(db: AsyncSession, user_id: UUID) -> int:
    today = datetime.now(timezone.utc).date()
    count = await db.execute(
        select(func.count()).select_from(SwipeAction).where(
            SwipeAction.swiper_id == user_id,
            func.date(SwipeAction.created_at) == today,
            SwipeAction.action == "SUPER_LIKE",
        )
    )
    return max(0, settings.SWIPE_DAILY_SUPERLIKE_LIMIT - count.scalar())


async def perform_swipe_action(
    db: AsyncSession,
    user: User,
    target_type: str,
    target_id: str,
    action: str,
    context_project_id: UUID | None = None,
    context_role_slot_id: UUID | None = None,
) -> dict[str, Any]:
    target_type = target_type.upper()
    action = action.upper()
    if target_type not in {"PROJECT", "USER"}:
        raise ValueError("Invalid swipe target type")
    if action not in {"PASS", "LIKE", "SUPER_LIKE"}:
        raise ValueError("Invalid swipe action")
    await _validate_swipe_target(
        db, user, target_type, target_id, context_project_id, context_role_slot_id
    )
    context_key = str(context_project_id) if context_project_id else "GLOBAL"

    existing_result = await db.execute(
        select(SwipeAction).where(
            SwipeAction.swiper_id == user.id,
            SwipeAction.target_type == target_type,
            SwipeAction.target_id == target_id,
            SwipeAction.context_key == context_key,
            SwipeAction.is_active == True,
        ).order_by(SwipeAction.created_at.desc()).limit(1)
    )
    existing_swipe = existing_result.scalar_one_or_none()
    if existing_swipe and existing_swipe.action == action:
        if action in ("LIKE", "SUPER_LIKE"):
            matched = await _check_match(
                db, user.id, target_type, target_id, context_project_id
            )
            if matched:
                return {"matched": True, "match_id": str(matched.id)}
        return {"matched": False, "message": f"Already recorded {action.lower()}"}

    if action == "LIKE":
        remaining = await get_daily_likes_remaining(db, user.id)
        if remaining <= 0:
            raise ValueError("Daily like limit reached")
    if action == "SUPER_LIKE":
        remaining = await get_daily_superlikes_remaining(db, user.id)
        if remaining <= 0:
            raise ValueError("Daily super like limit reached")

    if existing_swipe:
        existing_swipe.is_active = False

    swipe = SwipeAction(
        swiper_id=user.id,
        target_type=target_type,
        target_id=target_id,
        action=action,
        context_project_id=context_project_id,
        context_role_slot_id=context_role_slot_id,
        context_key=context_key,
    )
    db.add(swipe)
    await db.flush()

    if action == "PASS":
        return {"matched": False, "message": "Passed"}

    if action in ("LIKE", "SUPER_LIKE"):
        matched = await _check_match(
            db, user.id, target_type, target_id, context_project_id
        )
        if matched:
            return {"matched": True, "match_id": str(matched.id)}
        if action == "SUPER_LIKE":
            await _notify_super_like(
                db, user, target_type, target_id, context_project_id
            )

    return {"matched": False, "message": f"Recorded {action.lower()}"}


async def _notify_super_like(
    db: AsyncSession,
    user: User,
    target_type: str,
    target_id: str,
    context_project_id: UUID | None,
) -> None:
    if target_type == "PROJECT":
        project = await db.get(Project, UUID(target_id))
        recipient_id = project.owner_id if project else None
        body = f"{user.full_name or 'A contributor'} is especially interested in your project."
    else:
        recipient_id = UUID(target_id)
        project = await db.get(Project, context_project_id) if context_project_id else None
        project_name = project.title if project else "a project"
        body = f"{user.full_name or 'A project owner'} is especially interested in you for {project_name}."
    if not recipient_id:
        return
    db.add(
        Notification(
            user_id=recipient_id,
            type="SUPER_LIKE",
            title="New Super Like",
            body=body,
            related_id=str(context_project_id or target_id),
        )
    )
    db.add(
        OutboxEvent(
            event_type="PUSH_NOTIFICATION",
            payload={
                "user_id": str(recipient_id),
                "title": "New Super Like",
                "body": body,
                "data": {
                    "type": "SUPER_LIKE",
                    "project_id": str(context_project_id or target_id),
                },
            },
        )
    )


async def _validate_swipe_target(
    db: AsyncSession,
    user: User,
    target_type: str,
    target_id: str,
    context_project_id: UUID | None,
    context_role_slot_id: UUID | None,
) -> None:
    try:
        target_uuid = UUID(target_id)
    except ValueError as exc:
        raise ValueError("Invalid target id") from exc
    if target_type == "PROJECT":
        project = await db.get(Project, target_uuid)
        owner = await db.get(User, project.owner_id) if project else None
        if (
            not project
            or project.owner_id == user.id
            or project.status != "RECRUITING"
            or project.review_status != "APPROVED"
            or not owner
            or not owner.is_active
        ):
            raise ValueError("Project is not eligible for discovery")
        blocked_user_id = project.owner_id
    else:
        if context_project_id is None:
            raise ValueError("Owner swipes require a project context")
        project = await db.get(Project, context_project_id)
        target = await db.get(User, target_uuid)
        if (
            not project
            or project.owner_id != user.id
            or project.status != "RECRUITING"
            or not target
            or target.id == user.id
            or not target.is_active
            or not target.email_verified
        ):
            raise ValueError("Candidate is not eligible for this project")
        if context_role_slot_id:
            slot = await db.get(ProjectRoleSlot, context_role_slot_id)
            if not slot or slot.project_id != project.id or slot.filled >= slot.count:
                raise ValueError("Role slot is not available")
        blocked_user_id = target_uuid
    blocked = await db.scalar(
        select(UserBlock.id).where(
            or_(
                and_(UserBlock.blocker_id == user.id, UserBlock.blocked_id == blocked_user_id),
                and_(UserBlock.blocker_id == blocked_user_id, UserBlock.blocked_id == user.id),
            )
        )
    )
    if blocked:
        raise ValueError("Target is unavailable")


async def expire_queued_items(db: AsyncSession, user_id: UUID) -> None:
    now = datetime.now(timezone.utc)
    result = await db.execute(
        select(SwipeQueueItem).where(
            SwipeQueueItem.swiper_id == user_id,
            SwipeQueueItem.is_active == True,
            SwipeQueueItem.expires_at <= now,
        )
    )
    for item in result.scalars().all():
        existing_pass = await db.execute(
            select(SwipeAction).where(
                SwipeAction.swiper_id == user_id,
                SwipeAction.target_type == item.target_type,
                SwipeAction.target_id == item.target_id,
                SwipeAction.action == "PASS",
                SwipeAction.is_active == True,
            ).limit(1)
        )
        if not existing_pass.scalar_one_or_none():
            db.add(
                SwipeAction(
                    swiper_id=user_id,
                    target_type=item.target_type,
                    target_id=item.target_id,
                    action="PASS",
                )
            )
        item.is_active = False
        item.resolution = "EXPIRED"
        item.resolved_at = now


async def expire_all_queued_items() -> None:
    async with async_session() as db:
        user_ids = (
            await db.execute(
                select(SwipeQueueItem.swiper_id)
                .where(
                    SwipeQueueItem.is_active == True,
                    SwipeQueueItem.expires_at <= datetime.now(timezone.utc),
                )
                .distinct()
            )
        ).scalars().all()
        for user_id in user_ids:
            await expire_queued_items(db, user_id)
        await db.commit()


async def add_to_queue(
    db: AsyncSession,
    user: User,
    target_type: str,
    target_id: str,
    context_project_id: UUID | None = None,
) -> dict:
    target_type = target_type.upper()
    if target_type not in {"PROJECT", "USER"}:
        raise ValueError("Invalid queue target type")

    await expire_queued_items(db, user.id)
    await _validate_swipe_target(
        db, user, target_type, target_id, context_project_id, None
    )
    context_key = str(context_project_id) if context_project_id else "GLOBAL"

    existing_swipe = await db.execute(
        select(SwipeAction).where(
            SwipeAction.swiper_id == user.id,
            SwipeAction.target_type == target_type,
            SwipeAction.target_id == target_id,
            SwipeAction.context_key == context_key,
            SwipeAction.is_active == True,
        ).limit(1)
    )
    if existing_swipe.scalar_one_or_none():
        raise ValueError("This profile already has a swipe decision")

    existing_queue = await db.execute(
        select(SwipeQueueItem).where(
            SwipeQueueItem.swiper_id == user.id,
            SwipeQueueItem.target_type == target_type,
            SwipeQueueItem.target_id == target_id,
            SwipeQueueItem.context_key == context_key,
            SwipeQueueItem.is_active == True,
        ).limit(1)
    )
    existing_item = existing_queue.scalar_one_or_none()
    if existing_item:
        return {"message": "Already in queue", "queue_item_id": str(existing_item.id)}

    active_count = await db.scalar(
        select(func.count()).select_from(SwipeQueueItem).where(
            SwipeQueueItem.swiper_id == user.id,
            SwipeQueueItem.target_type == target_type,
            SwipeQueueItem.is_active == True,
        )
    )
    if (active_count or 0) >= QUEUE_LIMIT_PER_TYPE:
        label = "user profiles" if target_type == "USER" else "project profiles"
        raise ValueError(f"Queue is full for {label}")

    now = datetime.now(timezone.utc)
    item = SwipeQueueItem(
        swiper_id=user.id,
        target_type=target_type,
        target_id=target_id,
        context_project_id=context_project_id,
        context_key=context_key,
        expires_at=now + QUEUE_TTL,
    )
    db.add(item)
    await db.flush()
    return {"message": "Added to queue", "queue_item_id": str(item.id)}


async def get_queue(db: AsyncSession, user: User) -> dict:
    await expire_queued_items(db, user.id)
    result = await db.execute(
        select(SwipeQueueItem).where(
            SwipeQueueItem.swiper_id == user.id,
            SwipeQueueItem.is_active == True,
        ).order_by(SwipeQueueItem.queued_at.asc())
    )
    items = result.scalars().all()

    user_items = []
    project_items = []
    for item in items:
        payload = await _build_queue_item_payload(db, user, item)
        if item.target_type == "USER":
            user_items.append(payload)
        elif item.target_type == "PROJECT":
            project_items.append(payload)

    return {
        "user_profiles": user_items,
        "project_profiles": project_items,
        "user_capacity_remaining": max(0, QUEUE_LIMIT_PER_TYPE - len(user_items)),
        "project_capacity_remaining": max(0, QUEUE_LIMIT_PER_TYPE - len(project_items)),
    }


async def decide_queue_item(db: AsyncSession, user: User, queue_item_id: UUID, action: str) -> dict:
    action = action.upper()
    if action not in {"PASS", "LIKE", "SUPER_LIKE"}:
        raise ValueError("Invalid queue action")
    await expire_queued_items(db, user.id)

    item = await _get_active_queue_item(db, user.id, queue_item_id)
    result = await perform_swipe_action(
        db,
        user,
        item.target_type,
        item.target_id,
        action,
        item.context_project_id,
    )
    item.is_active = False
    item.resolution = action
    item.resolved_at = datetime.now(timezone.utc)
    result["message"] = result.get("message") or "Queue decision recorded"
    return result


async def remove_queue_item(db: AsyncSession, user: User, queue_item_id: UUID) -> dict:
    await expire_queued_items(db, user.id)
    item = await _get_active_queue_item(db, user.id, queue_item_id)
    item.is_active = False
    item.resolution = "REMOVED"
    item.resolved_at = datetime.now(timezone.utc)
    return {"message": "Removed from queue"}


async def _get_active_queue_item(db: AsyncSession, user_id: UUID, queue_item_id: UUID) -> SwipeQueueItem:
    result = await db.execute(
        select(SwipeQueueItem).where(
            SwipeQueueItem.id == queue_item_id,
            SwipeQueueItem.swiper_id == user_id,
            SwipeQueueItem.is_active == True,
        )
    )
    item = result.scalar_one_or_none()
    if not item:
        raise ValueError("Queue item not found")
    return item


async def _ensure_queue_target_exists(db: AsyncSession, target_type: str, target_id: str) -> None:
    try:
        target_uuid = UUID(target_id)
    except ValueError:
        raise ValueError("Invalid queue target id")

    if target_type == "PROJECT":
        project = await db.get(Project, target_uuid)
        if not project:
            raise ValueError("Project not found")
    else:
        profile = await db.scalar(select(UserProfile).where(UserProfile.user_id == target_uuid))
        if not profile:
            raise ValueError("Profile not found")


async def _build_queue_item_payload(db: AsyncSession, user: User, item: SwipeQueueItem) -> dict:
    now = datetime.now(timezone.utc)
    expires_at = _as_aware_utc(item.expires_at)
    payload = {
        "id": str(item.id),
        "target_type": item.target_type,
        "target_id": item.target_id,
        "queued_at": item.queued_at,
        "expires_at": item.expires_at,
        "seconds_remaining": max(0, int((expires_at - now).total_seconds())),
        "user_card": None,
        "project_card": None,
    }
    if item.target_type == "USER":
        payload["user_card"] = await _build_user_queue_card(db, user, UUID(item.target_id))
    elif item.target_type == "PROJECT":
        payload["project_card"] = await _build_project_queue_card(db, user, UUID(item.target_id))
    return payload


async def _build_user_queue_card(db: AsyncSession, user: User, user_id: UUID) -> dict | None:
    profile = await db.scalar(select(UserProfile).where(UserProfile.user_id == user_id))
    target_user = await db.get(
        User,
        user_id,
        options=[
            selectinload(User.roles),
            selectinload(User.skills),
            selectinload(User.interests),
            selectinload(User.profile),
        ],
    )
    if not profile or not target_user:
        return None

    roles = await db.execute(select(UserRole).where(UserRole.user_id == user_id).order_by(UserRole.ordering).limit(3))
    skills = await db.execute(select(UserSkill).where(UserSkill.user_id == user_id))
    owner_projects = await _get_owner_projects(db, user)
    match_score = calculate_user_score(user, target_user, profile, owner_projects)

    return {
        "user_id": str(profile.user_id),
        "display_name": profile.display_name,
        "avatar_url": target_user.avatar_url,
        "verified_student": target_user.verified_student,
        "university": target_user.university,
        "bio": profile.bio[:100] if profile.bio else None,
        "roles": [
            {"id": str(r.id), "role_name": r.role_name, "ordering": r.ordering}
            for r in roles.scalars()
        ],
        "skills": [
            {"id": str(s.id), "skill_name": s.skill_name, "level": s.level, "needs_improvement": s.needs_improvement}
            for s in skills.scalars()
        ],
        "location": profile.location,
        "github_url": profile.github_url,
        "reputation_score": profile.reputation_score,
        "projects_completed": profile.projects_completed,
        "match_score": round(match_score, 1),
    }


async def _build_project_queue_card(db: AsyncSession, user: User, project_id: UUID) -> dict | None:
    project = await db.scalar(
        select(Project)
        .options(
            selectinload(Project.role_slots)
            .selectinload(ProjectRoleSlot.skill_requirements_rows)
            .selectinload(ProjectRoleSkillRequirement.skill)
        )
        .where(Project.id == project_id)
    )
    if not project:
        return None

    owner = await db.get(User, project.owner_id, options=[selectinload(User.profile)])
    total_filled = sum(s.filled for s in project.role_slots)
    total_slots = sum(s.count for s in project.role_slots)
    match_score = calculate_project_score(user, project, owner)

    return {
        "project_id": str(project.id),
        "title": project.title,
        "field": project.field,
        "description": project.description[:300],
        "owner_name": owner.full_name if owner else "",
        "owner_avatar": owner.avatar_url if owner else None,
        "owner_verified": owner.verified_student if owner else False,
        "role_slots": [
            {
                "id": str(s.id),
                "role_name": s.role_name,
                "count": s.count,
                "filled": s.filled,
                "skill_requirements": s.skill_requirements,
            }
            for s in project.role_slots
        ],
        "work_mode": project.work_mode,
        "commitment_level": project.commitment_level,
        "start_date": project.start_date.isoformat() if project.start_date else None,
        "end_date": project.end_date.isoformat() if project.end_date else None,
        "total_slots": total_slots,
        "filled_slots": total_filled,
        "match_score": round(match_score, 1),
    }


def _as_aware_utc(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


async def _check_match(
    db: AsyncSession,
    swiper_id: UUID,
    target_type: str,
    target_id: str,
    context_project_id: UUID | None = None,
) -> Match | None:
    """
    Match occurs when:
    - Contributor likes a project AND the project owner has liked that contributor
    - OR project owner likes a contributor AND the contributor has liked the project
    """
    if target_type == "PROJECT":
        contributor_id = swiper_id
        project_id = UUID(target_id)
        project = await db.scalar(
            select(Project)
            .options(selectinload(Project.role_slots))
            .where(Project.id == project_id)
        )
        if not project:
            return None

        owner_liked = await db.execute(
            select(SwipeAction).where(
                SwipeAction.swiper_id == project.owner_id,
                SwipeAction.target_type == "USER",
                SwipeAction.target_id == str(contributor_id),
                SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
                SwipeAction.context_project_id == project_id,
                SwipeAction.is_active == True,
            ).order_by(SwipeAction.created_at.desc()).limit(1)
        )
        if owner_liked.scalar_one_or_none():
            return await _create_match(db, contributor_id, project_id, project.owner_id)
        return None

    elif target_type == "USER":
        owner_id = swiper_id
        contributor_id = UUID(target_id)

        if context_project_id is None:
            return None
        project = await db.get(Project, context_project_id)
        if not project or project.owner_id != owner_id:
            return None
        contributor_liked = await db.scalar(
            select(SwipeAction.id).where(
                SwipeAction.swiper_id == contributor_id,
                SwipeAction.target_type == "PROJECT",
                SwipeAction.target_id == str(context_project_id),
                SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
                SwipeAction.is_active == True,
            )
        )
        if contributor_liked:
            return await _create_match(
                db, contributor_id, context_project_id, owner_id
            )
        return None

    return None


async def _create_match(db: AsyncSession, user_id: UUID, project_id: UUID, owner_id: UUID) -> Match | None:
    existing = await db.execute(
        select(Match).where(
            Match.user_id == user_id,
            Match.project_id == project_id,
            Match.is_unmatched == False,
        )
    )
    existing_match = existing.scalar_one_or_none()
    if existing_match:
        return existing_match

    contributor = await db.scalar(
        select(User)
        .options(
            selectinload(User.roles),
            selectinload(User.roles)
            .selectinload(UserRole.role_skills)
            .selectinload(UserRoleSkill.skill),
            selectinload(User.skills),
            selectinload(User.interests),
            selectinload(User.profile),
        )
        .where(User.id == user_id)
        .execution_options(populate_existing=True)
    )
    project = await db.scalar(
        select(Project)
        .options(
            selectinload(Project.role_slots)
            .selectinload(ProjectRoleSlot.skill_requirements_rows)
            .selectinload(ProjectRoleSkillRequirement.skill)
        )
        .where(Project.id == project_id)
        .execution_options(populate_existing=True)
    )
    owner = await db.scalar(
        select(User)
        .options(selectinload(User.profile))
        .where(User.id == owner_id)
        .execution_options(populate_existing=True)
    )

    computed_score = 0.0
    score_explanation = None
    matched_role = None
    if contributor and project:
        computed_score, score_explanation, slot = calculate_project_score_details(
            contributor, project, owner
        )
        matched_role = slot.role_name if slot else None

    match = Match(
        user_id=user_id,
        project_id=project_id,
        owner_id=owner_id,
        match_score=computed_score,
        role_matched=matched_role,
        score_explanation=score_explanation,
    )
    db.add(match)
    await db.flush()

    chat = Chat(match_id=match.id)
    db.add(chat)

    notif_user = Notification(
        user_id=user_id,
        type="NEW_MATCH",
        title="You have a new match!",
        body=f"You matched with a project! Start chatting.",
        related_id=str(match.id),
    )
    db.add(notif_user)

    notif_owner = Notification(
        user_id=owner_id,
        type="NEW_MATCH",
        title="You have a new match!",
        body=f"A contributor matched with your project!",
        related_id=str(match.id),
    )
    db.add(notif_owner)

    db.add(
        OutboxEvent(
            event_type="PUSH_NOTIFICATION",
            payload={
                "user_id": str(user_id),
                "title": "New Match!",
                "body": "You matched with a project! Start chatting.",
            },
        )
    )
    db.add(
        OutboxEvent(
            event_type="PUSH_NOTIFICATION",
            payload={
                "user_id": str(owner_id),
                "title": "New Match!",
                "body": "A contributor matched with your project!",
            },
        )
    )

    return match


async def get_discover_cards(
    db: AsyncSession,
    user: User,
    mode: str,
    cursor: str | None = None,
    limit: int = 20,
    project_id: UUID | None = None,
) -> dict:
    mode = mode.upper()
    if mode == "CONTRIBUTOR":
        return await _discover_projects(db, user, cursor, limit)
    if mode == "OWNER":
        if project_id is None:
            raise ValueError("Owner discovery requires project_id")
        return await _discover_users(db, user, cursor, limit, project_id)
    raise ValueError("Mode must be CONTRIBUTOR or OWNER")


async def _get_exclusion_ids(
    db: AsyncSession,
    user_id: UUID,
    context_project_id: UUID | None = None,
) -> tuple[set[str], set[str]]:
    await expire_queued_items(db, user_id)
    seven_days_ago = datetime.now(timezone.utc) - timedelta(days=settings.PASS_COOLDOWN_DAYS)

    recent_passes = await db.execute(
        select(SwipeAction.target_id, SwipeAction.target_type).where(
            SwipeAction.swiper_id == user_id,
            SwipeAction.action == "PASS",
            SwipeAction.created_at > seven_days_ago,
            SwipeAction.context_key == (
                str(context_project_id) if context_project_id else "GLOBAL"
            ),
            SwipeAction.is_active == True,
        )
    )

    all_likes = await db.execute(
        select(SwipeAction.target_id, SwipeAction.target_type).where(
            SwipeAction.swiper_id == user_id,
            SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
            SwipeAction.context_key == (
                str(context_project_id) if context_project_id else "GLOBAL"
            ),
            SwipeAction.is_active == True,
        )
    )

    exclude_user_ids = set()
    exclude_project_ids = set()
    blocked_rows = await db.execute(
        select(UserBlock.blocker_id, UserBlock.blocked_id).where(
            or_(UserBlock.blocker_id == user_id, UserBlock.blocked_id == user_id)
        )
    )
    for blocker_id, blocked_id in blocked_rows.all():
        exclude_user_ids.add(str(blocked_id if blocker_id == user_id else blocker_id))
    for tgt_id, tgt_type in recent_passes.all():
        if tgt_type == "USER":
            exclude_user_ids.add(tgt_id)
        elif tgt_type == "PROJECT":
            exclude_project_ids.add(tgt_id)
    for tgt_id, tgt_type in all_likes.all():
        if tgt_type == "USER":
            exclude_user_ids.add(tgt_id)
        elif tgt_type == "PROJECT":
            exclude_project_ids.add(tgt_id)

    active_queue = await db.execute(
        select(SwipeQueueItem.target_id, SwipeQueueItem.target_type).where(
            SwipeQueueItem.swiper_id == user_id,
            SwipeQueueItem.is_active == True,
        )
    )
    for tgt_id, tgt_type in active_queue.all():
        if tgt_type == "USER":
            exclude_user_ids.add(tgt_id)
        elif tgt_type == "PROJECT":
            exclude_project_ids.add(tgt_id)

    active_matches = await db.execute(
        select(Match).where(
            or_(
                and_(Match.user_id == user_id, Match.is_unmatched == False),
                and_(Match.owner_id == user_id, Match.is_unmatched == False),
            )
        )
    )
    for match in active_matches.scalars():
        if context_project_id is None:
            exclude_project_ids.add(str(match.project_id))
            exclude_user_ids.add(str(match.owner_id))
        elif match.project_id == context_project_id:
            exclude_user_ids.add(str(match.user_id))

    return exclude_user_ids, exclude_project_ids


async def _get_owner_projects(db: AsyncSession, owner: User) -> list[Project]:
    result = await db.execute(
        select(Project)
        .options(
            selectinload(Project.role_slots)
            .selectinload(ProjectRoleSlot.skill_requirements_rows)
            .selectinload(ProjectRoleSkillRequirement.skill)
        )
        .where(
            Project.owner_id == owner.id,
            Project.status == "RECRUITING",
            Project.review_status == "APPROVED",
        )
    )
    return list(result.scalars().all())


async def _discover_projects(
    db: AsyncSession, user: User, cursor: str | None, limit: int
) -> dict:
    exclude_user_ids, exclude_project_ids = await _get_exclusion_ids(db, user.id)

    vector = _get_user_search_vector(user)
    project_ids = None
    if vector:
        project_ids = _search_similar_projects(vector, limit * 2, exclude_project_ids)

    query = (
        select(Project)
        .options(
            selectinload(Project.role_slots)
            .selectinload(ProjectRoleSlot.skill_requirements_rows)
            .selectinload(ProjectRoleSkillRequirement.skill)
        )
        .join(User, User.id == Project.owner_id)
        .where(
            Project.status == "RECRUITING",
            Project.review_status == "APPROVED",
            Project.owner_id != user.id,
            User.is_active == True,
            User.email_verified == True,
            ~Project.id.in_([UUID(p) for p in exclude_project_ids if p] if exclude_project_ids else [UUID("00000000-0000-0000-0000-000000000000")]),
        )
    )
    if project_ids:
        query = query.where(Project.id.in_(project_ids))
    projects = (await db.execute(query)).scalars().all()

    cards = []
    for project in projects:
        owner = await db.get(
            User,
            project.owner_id,
            options=[selectinload(User.profile)]
        )

        total_filled = sum(s.filled for s in project.role_slots)
        total_slots = sum(s.count for s in project.role_slots)

        match_score, explanation, matched_slot = calculate_project_score_details(
            user, project, owner
        )

        if vector:
            project_vec = _get_or_compute_embedding("project", str(project.id), _build_project_text(project))
            if project_vec:
                vec_score = _cosine_similarity(vector, project_vec) * 100
                match_score = round((match_score + vec_score) / 2, 1)

        card = {
            "project_id": str(project.id),
            "title": project.title,
            "field": project.field,
            "description": project.description[:300],
            "owner_name": owner.full_name if owner else "",
            "owner_avatar": owner.avatar_url if owner else None,
            "owner_verified": owner.verified_student if owner else False,
            "role_slots": [
                {
                    "id": str(s.id),
                    "role_name": s.role_name,
                    "count": s.count,
                    "filled": s.filled,
                    "skill_requirements": s.skill_requirements,
                }
                for s in project.role_slots
            ],
            "work_mode": project.work_mode,
            "commitment_level": project.commitment_level,
            "start_date": project.start_date.isoformat() if project.start_date else None,
            "end_date": project.end_date.isoformat() if project.end_date else None,
            "total_slots": total_slots,
            "filled_slots": total_filled,
            "match_score": round(match_score, 1),
            "matched_role": matched_slot.role_name if matched_slot else None,
            "score_explanation": explanation,
        }
        cards.append(card)

    cards.sort(key=lambda item: (-item["match_score"], item["project_id"]))
    offset = _cursor_offset(cursor)
    page = cards[offset:offset + limit]
    next_cursor = str(offset + limit) if offset + limit < len(cards) else None

    return {
        "project_cards": page,
        "user_cards": [],
        "next_cursor": next_cursor,
        "daily_likes_remaining": await get_daily_likes_remaining(db, user.id),
        "daily_superlikes_remaining": await get_daily_superlikes_remaining(db, user.id),
    }


async def _discover_users(
    db: AsyncSession,
    user: User,
    cursor: str | None,
    limit: int,
    project_id: UUID,
) -> dict:
    project = await db.scalar(
        select(Project)
        .options(
            selectinload(Project.role_slots)
            .selectinload(ProjectRoleSlot.skill_requirements_rows)
            .selectinload(ProjectRoleSkillRequirement.skill)
        )
        .where(
            Project.id == project_id,
            Project.owner_id == user.id,
            Project.status == "RECRUITING",
            Project.review_status == "APPROVED",
        )
    )
    if not project:
        raise ValueError("Recruiting project not found")
    exclude_user_ids, exclude_project_ids = await _get_exclusion_ids(
        db, user.id, project_id
    )
    blocked_rows = await db.execute(
        select(UserBlock.blocker_id, UserBlock.blocked_id).where(
            or_(UserBlock.blocker_id == user.id, UserBlock.blocked_id == user.id)
        )
    )
    for blocker_id, blocked_id in blocked_rows.all():
        exclude_user_ids.add(str(blocked_id if blocker_id == user.id else blocker_id))

    vector = _get_user_search_vector(user)
    profile_ids = None
    if vector:
        profile_ids = _search_similar_users(vector, limit * 2, exclude_user_ids)

    query = (
        select(UserProfile)
        .join(User, User.id == UserProfile.user_id)
        .where(
            UserProfile.mode.in_(["CONTRIBUTOR", "BOTH"]),
            UserProfile.is_hidden == False,
            UserProfile.user_id != user.id,
            User.is_active == True,
            User.email_verified == True,
            ~UserProfile.user_id.in_([UUID(u) for u in exclude_user_ids if u] if exclude_user_ids else [UUID("00000000-0000-0000-0000-000000000000")]),
        )
    )
    if profile_ids:
        query = query.where(UserProfile.user_id.in_(profile_ids))
    profiles = (await db.execute(query)).scalars().all()

    cards = []
    for profile in profiles:
        pu = await db.get(
            User,
            profile.user_id,
            options=[
                selectinload(User.roles),
                selectinload(User.roles)
                .selectinload(UserRole.role_skills)
                .selectinload(UserRoleSkill.skill),
                selectinload(User.skills),
                selectinload(User.interests),
                selectinload(User.profile),
            ]
        )
        roles = await db.execute(
            select(UserRole).where(UserRole.user_id == profile.user_id).order_by(UserRole.ordering).limit(3)
        )
        skills = await db.execute(
            select(UserSkill).where(UserSkill.user_id == profile.user_id)
        )

        match_score, explanation, matched_slot = calculate_user_score_details(
            user, pu, project
        )

        if vector:
            profile_vec = _get_or_compute_embedding("user", str(profile.user_id), _build_user_text(profile))
            if profile_vec:
                vec_score = _cosine_similarity(vector, profile_vec) * 100
                match_score = round((match_score + vec_score) / 2, 1)

        card = {
            "user_id": str(profile.user_id),
            "display_name": profile.display_name,
            "avatar_url": pu.avatar_url if pu else None,
            "verified_student": pu.verified_student if pu else False,
            "university": pu.university if pu else None,
            "bio": profile.bio[:100] if profile.bio else None,
            "roles": [
                {"id": str(r.id), "role_name": r.role_name, "ordering": r.ordering}
                for r in roles.scalars()
            ],
            "skills": [
                {"id": str(s.id), "skill_name": s.skill_name, "level": s.level, "needs_improvement": s.needs_improvement}
                for s in skills.scalars()
            ],
            "location": profile.location,
            "github_url": profile.github_url,
            "reputation_score": profile.reputation_score,
            "projects_completed": profile.projects_completed,
            "match_score": round(match_score, 1),
            "matched_role": matched_slot.role_name if matched_slot else None,
            "score_explanation": explanation,
        }
        cards.append(card)

    cards.sort(key=lambda item: (-item["match_score"], item["user_id"]))
    offset = _cursor_offset(cursor)
    page = cards[offset:offset + limit]
    next_cursor = str(offset + limit) if offset + limit < len(cards) else None

    return {
        "user_cards": page,
        "project_cards": [],
        "next_cursor": next_cursor,
        "context_project_id": str(project_id),
        "daily_likes_remaining": await get_daily_likes_remaining(db, user.id),
        "daily_superlikes_remaining": await get_daily_superlikes_remaining(db, user.id),
    }


def _cursor_offset(cursor: str | None) -> int:
    if not cursor:
        return 0
    try:
        return max(0, int(cursor))
    except ValueError as exc:
        raise ValueError("Invalid discovery cursor") from exc


def _get_user_search_vector(user: User) -> list[float] | None:
    if not settings.ENABLE_EMBEDDINGS:
        return None
    try:
        parts = [
            user.full_name or "",
            user.profile.bio or "" if user.profile else "",
            ", ".join(r.role_name for r in user.roles),
            ", ".join(f"{s.skill_name}({s.level})" for s in user.skills),
            user.university or "",
        ]
        text = " | ".join(filter(None, parts))
        if not text.strip():
            return None
        return encode_text(text)
    except Exception as e:
        logger.error(f"Failed to encode search vector: {e}")
        return None


def _search_similar_projects(vector: list[float], limit: int, exclude_ids: set[str]) -> list[UUID] | None:
    if not settings.ENABLE_MILVUS:
        return None
    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("project_vectors")
        collection.load()

        expr = "status == 'RECRUITING'"
        search_params = {"metric_type": "COSINE", "params": {"ef": 64}}
        results = collection.search(
            data=[vector],
            anns_field="vector",
            param=search_params,
            limit=limit,
            expr=expr,
            output_fields=["project_id"],
        )
        disconnect_milvus()

        ids = []
        for hit in results[0]:
            pid = hit.entity.get("project_id")
            if pid and pid not in exclude_ids:
                ids.append(UUID(pid))
        return ids[:limit]
    except Exception as e:
        logger.error(f"Milvus project search failed: {e}")
        disconnect_milvus()
        return None


def _search_similar_users(vector: list[float], limit: int, exclude_ids: set[str]) -> list[UUID] | None:
    if not settings.ENABLE_MILVUS:
        return None
    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("user_profile_vectors")
        collection.load()

        expr = "is_active == true and mode in ['CONTRIBUTOR', 'BOTH']"
        search_params = {"metric_type": "COSINE", "params": {"ef": 64}}
        results = collection.search(
            data=[vector],
            anns_field="vector",
            param=search_params,
            limit=limit,
            expr=expr,
            output_fields=["user_id"],
        )
        disconnect_milvus()

        ids = []
        for hit in results[0]:
            uid = hit.entity.get("user_id")
            if uid and uid not in exclude_ids:
                ids.append(UUID(uid))
        return ids[:limit]
    except Exception as e:
        logger.error(f"Milvus user search failed: {e}")
        disconnect_milvus()
        return None


def _cosine_similarity(a: list[float], b: list[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = sum(x * x for x in a) ** 0.5
    norm_b = sum(x * x for x in b) ** 0.5
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return max(0.0, min(1.0, dot / (norm_a * norm_b)))


_embedding_cache: dict[str, list[float]] = {}


def _get_or_compute_embedding(entity_type: str, entity_id: str, text: str) -> list[float] | None:
    if not settings.ENABLE_EMBEDDINGS:
        return None
    cache_key = f"{entity_type}:{entity_id}"
    if cache_key in _embedding_cache:
        return _embedding_cache[cache_key]
    try:
        vec = encode_text(text)
        _embedding_cache[cache_key] = vec
        return vec
    except Exception:
        return None


def _build_user_text(profile) -> str:
    parts = [
        profile.display_name or "",
        profile.bio or "",
    ]
    return " | ".join(filter(None, parts))


def _build_project_text(project) -> str:
    parts = [
        project.title or "",
        project.field or "",
        ", ".join(slot.role_name for slot in project.role_slots),
    ]
    return " | ".join(filter(None, parts))


def _calculate_text_similarity(text1: str, text2: str) -> float:
    words1 = set(text1.lower().split())
    words2 = set(text2.lower().split())
    if not words1 or not words2:
        return 0.0
    intersection = words1 & words2
    union = words1 | words2
    jaccard = len(intersection) / len(union) if union else 0.0
    score = jaccard * 100
    return min(score, 99.0)


async def get_matches(db: AsyncSession, user: User) -> list[dict]:
    result = await db.execute(
        select(Match).where(
            or_(
                and_(Match.user_id == user.id, Match.is_unmatched == False),
                and_(Match.owner_id == user.id, Match.is_unmatched == False),
            )
        ).order_by(Match.matched_at.desc())
    )
    matches = result.scalars().all()

    response = []
    for match in matches:
        other_user_id = match.user_id if match.owner_id == user.id else match.owner_id
        other_user = await db.get(User, other_user_id)
        project = await db.get(Project, match.project_id)
        chat_result = await db.execute(select(Chat).where(Chat.match_id == match.id))
        chat = chat_result.scalar_one_or_none()

        last_message = None
        unread_count = 0
        presence = presence_manager.get_presence(str(other_user_id))
        if chat:
            last_message_result = await db.execute(
                select(Message).where(Message.chat_id == chat.id).order_by(Message.created_at.desc()).limit(1)
            )
            last_message = last_message_result.scalar_one_or_none()
            unread_result = await db.execute(
                select(func.count()).select_from(Message).where(
                    Message.chat_id == chat.id,
                    Message.sender_id != user.id,
                    Message.is_read == False,
                )
            )
            unread_count = unread_result.scalar() or 0

        response.append({
            "id": str(match.id),
            "user_id": str(match.user_id),
            "project_id": str(match.project_id),
            "owner_id": str(match.owner_id),
            "other_user_id": str(other_user_id),
            "role_matched": match.role_matched,
            "match_score": match.match_score,
            "matched_at": match.matched_at,
            "is_unmatched": match.is_unmatched,
            "is_member_added": match.is_member_added,
            "user_name": other_user.full_name if other_user else "",
            "user_avatar": other_user.avatar_url if other_user else None,
            "user_is_online": presence["is_online"],
            "user_last_seen_at": presence["last_seen_at"],
            "project_title": project.title if project else "",
            "last_message": last_message.content[:100] if last_message else None,
            "last_message_time": last_message.created_at.isoformat() if last_message else None,
            "is_unread": unread_count > 0,
        })

    return response


async def unmatch(db: AsyncSession, user: User, match_id: UUID) -> dict:
    match = await db.get(Match, match_id)
    if not match:
        raise Exception("Match not found")
    if user.id not in (match.user_id, match.owner_id):
        raise Exception("Not authorized")

    match.is_unmatched = True
    match.unmatched_at = datetime.now(timezone.utc)
    match.chat_hidden_until = datetime.now(timezone.utc) + timedelta(days=30)

    return {"message": "Unmatched successfully"}


async def get_applicants_for_project(
    db: AsyncSession, user: User, project_id: UUID
) -> list[dict]:
    project = await db.get(Project, project_id)
    if not project or project.owner_id != user.id:
        raise Exception("Project not found or not authorized")

    result = await db.execute(
        select(SwipeAction).where(
            SwipeAction.target_type == "PROJECT",
            SwipeAction.target_id == str(project_id),
            SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
            SwipeAction.is_active == True,
        ).order_by(SwipeAction.created_at.desc())
    )
    swipes = result.scalars().all()

    applicants = []
    seen_user_ids: set[UUID] = set()
    for swipe in swipes:
        swiper_id = UUID(str(swipe.swiper_id))
        if swiper_id in seen_user_ids:
            continue
        seen_user_ids.add(swiper_id)
        swiper = await db.get(
            User,
            swiper_id,
            options=[
                selectinload(User.roles),
                selectinload(User.roles)
                .selectinload(UserRole.role_skills)
                .selectinload(UserRoleSkill.skill),
                selectinload(User.skills),
                selectinload(User.interests),
                selectinload(User.profile),
            ],
        )
        if not swiper or not swiper.is_active or not swiper.email_verified:
            continue
        profile_result = await db.execute(
            select(UserProfile).where(UserProfile.user_id == swiper.id)
        )
        profile = profile_result.scalar_one_or_none()
        roles_result = await db.execute(
            select(UserRole).where(UserRole.user_id == swiper.id)
        )
        skills_result = await db.execute(
            select(UserSkill).where(UserSkill.user_id == swiper.id)
        )
        score, explanation, matched_slot = calculate_user_score_details(
            user, swiper, project
        )

        role_rows = roles_result.scalars().all()
        skill_rows = skills_result.scalars().all()
        applicants.append({
            "user_id": str(swiper.id),
            "display_name": profile.display_name if profile else swiper.full_name,
            "avatar_url": swiper.avatar_url,
            "roles": [
                {
                    "id": str(role.id),
                    "role_name": role.role_name,
                    "ordering": role.ordering,
                    "skills": [],
                }
                for role in role_rows
            ],
            "skills": [
                {
                    "id": str(skill.id),
                    "skill_name": skill.skill_name,
                    "level": skill.level,
                    "needs_improvement": skill.needs_improvement,
                }
                for skill in skill_rows
            ],
            "verified_student": swiper.verified_student,
            "reputation_score": profile.reputation_score if profile else 3.0,
            "match_score": round(score, 1),
            "matched_role": matched_slot.role_name if matched_slot else None,
            "score_explanation": explanation,
            "is_super_like": swipe.action == "SUPER_LIKE",
            "swiped_at": swipe.created_at,
        })

    applicants.sort(
        key=lambda item: (
            not item["is_super_like"],
            -item["match_score"],
            item["display_name"].lower(),
        )
    )
    return applicants


# Need this import at the bottom to avoid circular imports
from app.models.chat import Message
