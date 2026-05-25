import logging
from datetime import datetime, timedelta, timezone
from uuid import UUID
from typing import Any

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_, or_, delete, func
from sqlalchemy.orm import selectinload
from pymilvus import Collection

from app.config import get_settings
from app.milvus_client import connect_milvus, disconnect_milvus
from app.models.swipe import SwipeAction, Match
from app.models.project import Project, ProjectRoleSlot
from app.models.user import User
from app.models.profile import UserProfile, UserRole, UserSkill
from app.models.chat import Chat, Notification, Message
from app.services.embedding_service import encode_text
from app.services.fcm_service import notify_user
from app.services.presence_service import presence_manager
from app.services.matching_service import (
    calculate_project_score,
    calculate_user_score,
    calculate_match_score_for_pair,
)

logger = logging.getLogger(__name__)
settings = get_settings()


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
    db: AsyncSession, user: User, target_type: str, target_id: str, action: str
) -> dict[str, Any]:
    if action == "LIKE":
        remaining = await get_daily_likes_remaining(db, user.id)
        if remaining <= 0:
            return {"matched": False, "message": "Daily like limit reached"}
    if action == "SUPER_LIKE":
        remaining = await get_daily_superlikes_remaining(db, user.id)
        if remaining <= 0:
            return {"matched": False, "message": "Daily super like limit reached"}

    swipe = SwipeAction(
        swiper_id=user.id,
        target_type=target_type,
        target_id=target_id,
        action=action,
    )
    db.add(swipe)

    if action == "PASS":
        return {"matched": False, "message": "Passed"}

    if action in ("LIKE", "SUPER_LIKE"):
        matched = await _check_match(db, user.id, target_type, target_id)
        if matched:
            return {"matched": True, "match_id": str(matched.id)}

    return {"matched": False, "message": f"Recorded {action.lower()}"}


async def _check_match(db: AsyncSession, swiper_id: UUID, target_type: str, target_id: str) -> Match | None:
    """
    Match occurs when:
    - Contributor likes a project AND the project owner has liked that contributor
    - OR project owner likes a contributor AND the contributor has liked the project
    """
    if target_type == "PROJECT":
        contributor_id = swiper_id
        project_id = UUID(target_id)
        project = await db.get(Project, project_id)
        if not project:
            return None

        owner_liked = await db.execute(
            select(SwipeAction).where(
                SwipeAction.swiper_id == project.owner_id,
                SwipeAction.target_type == "USER",
                SwipeAction.target_id == str(contributor_id),
                SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
            ).order_by(SwipeAction.created_at.desc()).limit(1)
        )
        if owner_liked.scalar_one_or_none():
            return await _create_match(db, contributor_id, project_id, project.owner_id)
        return None

    elif target_type == "USER":
        owner_id = swiper_id
        contributor_id = UUID(target_id)

        contributor_liked_projects = await db.execute(
            select(SwipeAction).where(
                SwipeAction.swiper_id == contributor_id,
                SwipeAction.target_type == "PROJECT",
                SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
            )
        )
        liked_project_ids = [UUID(s.target_id) for s in contributor_liked_projects.scalars().all()]

        for pid in liked_project_ids:
            project = await db.get(Project, pid)
            if project and project.owner_id == owner_id:
                return await _create_match(db, contributor_id, pid, owner_id)

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
    if existing.scalar_one_or_none():
        return None

    contributor = await db.get(User, user_id, options=[selectinload(User.roles), selectinload(User.skills), selectinload(User.interests), selectinload(User.profile)])
    project = await db.get(Project, project_id, options=[selectinload(Project.role_slots)])

    computed_score = 0.0
    if contributor and project:
        computed_score = calculate_match_score_for_pair(contributor, project)

    match = Match(
        user_id=user_id,
        project_id=project_id,
        owner_id=owner_id,
        match_score=computed_score,
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

    import asyncio
    asyncio.create_task(notify_user(db, user_id, "New Match!", "You matched with a project! Start chatting."))
    asyncio.create_task(notify_user(db, owner_id, "New Match!", "A contributor matched with your project!"))

    return match


async def get_discover_cards(
    db: AsyncSession, user: User, mode: str, cursor: str | None = None, limit: int = 20
) -> dict:
    if mode == "CONTRIBUTOR":
        return await _discover_projects(db, user, cursor, limit)
    else:
        return await _discover_users(db, user, cursor, limit)


async def _get_exclusion_ids(db: AsyncSession, user_id: UUID) -> tuple[set[str], set[str]]:
    seven_days_ago = datetime.now(timezone.utc) - timedelta(days=settings.PASS_COOLDOWN_DAYS)

    recent_passes = await db.execute(
        select(SwipeAction.target_id, SwipeAction.target_type).where(
            SwipeAction.swiper_id == user_id,
            SwipeAction.action == "PASS",
            SwipeAction.created_at > seven_days_ago,
        )
    )

    all_likes = await db.execute(
        select(SwipeAction.target_id, SwipeAction.target_type).where(
            SwipeAction.swiper_id == user_id,
            SwipeAction.action.in_(["LIKE", "SUPER_LIKE"]),
        )
    )

    exclude_user_ids = set()
    exclude_project_ids = set()
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

    active_matches = await db.execute(
        select(Match).where(
            or_(
                and_(Match.user_id == user_id, Match.is_unmatched == False),
                and_(Match.owner_id == user_id, Match.is_unmatched == False),
            )
        )
    )
    for match in active_matches.scalars():
        exclude_project_ids.add(str(match.project_id))
        exclude_user_ids.add(str(match.user_id))
        exclude_user_ids.add(str(match.owner_id))

    return exclude_user_ids, exclude_project_ids


async def _get_owner_projects(db: AsyncSession, owner: User) -> list[Project]:
    result = await db.execute(
        select(Project)
        .options(selectinload(Project.role_slots))
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
        .options(selectinload(Project.role_slots))
        .where(
            Project.status == "RECRUITING",
            Project.review_status == "APPROVED",
            ~Project.id.in_([UUID(p) for p in exclude_project_ids if p] if exclude_project_ids else [UUID("00000000-0000-0000-0000-000000000000")]),
        )
    )
    if project_ids:
        query = query.where(Project.id.in_(project_ids))
    query = query.limit(limit * 2)

    projects = (await db.execute(query)).scalars().all()

    cards = []
    for project in projects:
        owner = await db.get(User, project.owner_id)

        total_filled = sum(s.filled for s in project.role_slots)
        total_slots = sum(s.count for s in project.role_slots)

        match_score = calculate_project_score(user, project, owner)

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
        }
        cards.append(card)

    return {
        "project_cards": cards,
        "user_cards": [],
        "next_cursor": None,
        "daily_likes_remaining": await get_daily_likes_remaining(db, user.id),
        "daily_superlikes_remaining": await get_daily_superlikes_remaining(db, user.id),
    }


async def _discover_users(
    db: AsyncSession, user: User, cursor: str | None, limit: int
) -> dict:
    exclude_user_ids, exclude_project_ids = await _get_exclusion_ids(db, user.id)

    vector = _get_user_search_vector(user)
    profile_ids = None
    if vector:
        profile_ids = _search_similar_users(vector, limit * 2, exclude_user_ids)

    query = (
        select(UserProfile)
        .where(
            UserProfile.mode.in_(["CONTRIBUTOR", "BOTH"]),
            UserProfile.is_hidden == False,
            UserProfile.user_id != user.id,
            ~UserProfile.user_id.in_([UUID(u) for u in exclude_user_ids if u] if exclude_user_ids else [UUID("00000000-0000-0000-0000-000000000000")]),
        )
    )
    if profile_ids:
        query = query.where(UserProfile.user_id.in_(profile_ids))
    query = query.limit(limit * 2)

    profiles = (await db.execute(query)).scalars().all()

    owner_projects = await _get_owner_projects(db, user)

    cards = []
    for profile in profiles:
        pu = await db.get(User, profile.user_id)
        roles = await db.execute(
            select(UserRole).where(UserRole.user_id == profile.user_id).order_by(UserRole.ordering).limit(3)
        )
        skills = await db.execute(
            select(UserSkill).where(UserSkill.user_id == profile.user_id)
        )

        match_score = calculate_user_score(user, pu, profile, owner_projects)

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
        }
        cards.append(card)

    return {
        "user_cards": cards,
        "project_cards": [],
        "next_cursor": None,
        "daily_likes_remaining": await get_daily_likes_remaining(db, user.id),
        "daily_superlikes_remaining": await get_daily_superlikes_remaining(db, user.id),
    }


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
        ).order_by(SwipeAction.created_at.desc())
    )
    swipes = result.scalars().all()

    applicants = []
    for swipe in swipes:
        swiper = await db.get(User, UUID(swipe.swiper_id))
        if not swiper:
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

        applicants.append({
            "user_id": str(swiper.id),
            "display_name": profile.display_name if profile else swiper.full_name,
            "avatar_url": swiper.avatar_url,
            "roles": [{"role_name": r.role_name} for r in roles_result.scalars()],
            "skills": [{"skill_name": s.skill_name, "level": s.level} for s in skills_result.scalars()],
            "verified_student": swiper.verified_student,
            "reputation_score": profile.reputation_score if profile else 3.0,
            "match_score": 0.0,
            "swiped_at": swipe.created_at,
        })

    return applicants


# Need this import at the bottom to avoid circular imports
from app.models.chat import Message
