from datetime import datetime, timezone
from uuid import UUID
from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import get_db
from app.core.dependencies import get_current_user
from app.core.security import decode_token
from app.models.user import User
from app.models.swipe import Match
from app.models.chat import Chat, Message, Notification, ProjectInvitation
from app.models.project import Project, ProjectMember, ProjectRoleSlot
from app.schemas.chat import ProjectInvitationCreate
from app.api.websocket import manager
from app.services.notification_service import get_notifications, get_unread_notification_count
from app.services.presence_service import presence_manager

router = APIRouter(prefix="/api/v1", tags=["chat"])


async def _get_authorized_match(db: AsyncSession, match_id: UUID, user: User) -> Match:
    match = await db.get(Match, match_id)
    if not match or user.id not in (match.user_id, match.owner_id) or match.is_unmatched:
        raise HTTPException(status_code=403, detail="Not authorized")
    return match


async def _build_invitation_response(db: AsyncSession, invitation: ProjectInvitation, current_user: User) -> dict:
    project = await db.get(Project, invitation.project_id)
    inviter = await db.get(User, invitation.inviter_id)
    invitee = await db.get(User, invitation.invitee_id)
    return {
        "id": str(invitation.id),
        "match_id": str(invitation.match_id),
        "project_id": str(invitation.project_id),
        "project_title": project.title if project else "",
        "inviter_id": str(invitation.inviter_id),
        "inviter_name": inviter.full_name if inviter else "",
        "invitee_id": str(invitation.invitee_id),
        "invitee_name": invitee.full_name if invitee else "",
        "role_slot_id": str(invitation.role_slot_id) if invitation.role_slot_id else None,
        "role": invitation.role,
        "message": invitation.message,
        "status": invitation.status,
        "created_at": invitation.created_at.isoformat() if invitation.created_at else "",
        "responded_at": invitation.responded_at.isoformat() if invitation.responded_at else None,
        "is_incoming": invitation.invitee_id == current_user.id,
        "is_outgoing": invitation.inviter_id == current_user.id,
    }


async def _add_member_from_invitation(db: AsyncSession, invitation: ProjectInvitation) -> None:
    project = await db.get(Project, invitation.project_id)
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    if project.review_status != "APPROVED":
        raise HTTPException(status_code=400, detail="Cannot add members to a project that is not approved")

    existing = await db.execute(
        select(ProjectMember).where(
            ProjectMember.project_id == invitation.project_id,
            ProjectMember.user_id == invitation.invitee_id,
        )
    )
    if existing.scalar_one_or_none():
        return

    member_count_result = await db.execute(
        select(func.count()).select_from(ProjectMember).where(ProjectMember.project_id == invitation.project_id)
    )
    member_count = member_count_result.scalar() or 0
    if member_count >= project.max_members:
        raise HTTPException(status_code=400, detail="Project is already full")

    slot = await db.get(ProjectRoleSlot, invitation.role_slot_id) if invitation.role_slot_id else None
    if not slot or slot.project_id != invitation.project_id:
        raise HTTPException(status_code=404, detail="Role slot not found")
    if slot.filled >= slot.count:
        raise HTTPException(status_code=400, detail="Selected role slot is already filled")

    db.add(ProjectMember(project_id=invitation.project_id, user_id=invitation.invitee_id, role=invitation.role))
    await db.flush()
    slot.filled += 1

    match = await db.get(Match, invitation.match_id)
    if match:
        match.is_member_added = True
        match.role_matched = invitation.role

    slots_result = await db.execute(select(ProjectRoleSlot).where(ProjectRoleSlot.project_id == invitation.project_id))
    slots = slots_result.scalars().all()
    all_slots_filled = bool(slots) and all(s.filled >= s.count for s in slots)
    if member_count + 1 >= project.max_members or all_slots_filled:
        project.status = "ACTIVE"


@router.get("/chat/inbox")
async def get_inbox(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    matches_result = await db.execute(
        select(Match).where(
            ((Match.user_id == current_user.id) | (Match.owner_id == current_user.id)) &
            (Match.is_unmatched == False)
        ).order_by(Match.matched_at.desc())
    )
    matches = matches_result.scalars().all()

    chats = []
    for match in matches:
        other_user_id = match.user_id if match.owner_id == current_user.id else match.owner_id
        other_user = await db.get(User, other_user_id)
        project = await db.get(Project, match.project_id)
        presence = presence_manager.get_presence(str(other_user_id))

        chat_result = await db.execute(
            select(Chat).where(Chat.match_id == match.id)
        )
        chat = chat_result.scalar_one_or_none()

        last_msg_result = await db.execute(
            select(Message).where(
                Message.chat_id == chat.id if chat else None
            ).order_by(Message.created_at.desc()).limit(1)
        ) if chat else None

        unread_count = 0
        if chat:
            unread_result = await db.execute(
                select(func.count()).select_from(Message).where(
                    Message.chat_id == chat.id,
                    Message.sender_id != current_user.id,
                    Message.is_read == False,
                )
            )
            unread_count = unread_result.scalar() or 0

        last_msg = last_msg_result.scalar_one_or_none() if last_msg_result else None

        chats.append({
            "id": str(chat.id) if chat else "",
            "match_id": str(match.id),
            "user_id": str(other_user_id),
            "user_name": other_user.full_name if other_user else "",
            "user_avatar": other_user.avatar_url if other_user else None,
            "user_is_online": presence["is_online"],
            "user_last_seen_at": presence["last_seen_at"],
            "project_title": project.title if project else "",
            "last_message": last_msg.content[:100] if last_msg else None,
            "last_message_time": last_msg.created_at.isoformat() if last_msg else None,
            "is_unread": unread_count > 0,
            "unread_count": unread_count,
            "matched_at": match.matched_at.isoformat(),
        })

    return sorted(
        chats,
        key=lambda item: item["last_message_time"] or item["matched_at"],
        reverse=True,
    )


@router.get("/chat/{match_id}/messages")
async def get_messages(
    match_id: UUID,
    limit: int = 50,
    before: str | None = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    match = await db.get(Match, match_id)
    if not match or (match.user_id != current_user.id and match.owner_id != current_user.id):
        raise HTTPException(status_code=403, detail="Not authorized")

    chat_result = await db.execute(select(Chat).where(Chat.match_id == match_id))
    chat = chat_result.scalar_one_or_none()
    if not chat:
        return []

    limit = min(max(limit, 1), 100)
    query = select(Message).where(Message.chat_id == chat.id)
    if before:
        try:
            before_dt = datetime.fromisoformat(before.replace("Z", "+00:00"))
            query = query.where(Message.created_at < before_dt)
        except ValueError:
            raise HTTPException(status_code=400, detail="Invalid before timestamp")

    query = query.order_by(Message.created_at.desc()).limit(limit)
    messages = (await db.execute(query)).scalars().all()

    unread_result = await db.execute(
        select(Message).where(
            Message.chat_id == chat.id,
            Message.sender_id != current_user.id,
            Message.is_read == False,
        )
    )
    unread_messages = unread_result.scalars().all()
    for msg in unread_messages:
        msg.is_read = True
    if unread_messages:
        await db.commit()
        other_user_id = match.user_id if match.owner_id == current_user.id else match.owner_id
        await manager.send_message(str(other_user_id), str(match_id), {
            "type": "read_receipt",
            "by": str(current_user.id),
        })

    response = []
    for msg in reversed(messages):
        sender = await db.get(User, msg.sender_id)
        response.append({
            "id": str(msg.id),
            "chat_id": str(msg.chat_id),
            "sender_id": str(msg.sender_id),
            "content": msg.content,
            "is_read": msg.is_read,
            "created_at": msg.created_at.isoformat(),
            "sender_name": sender.full_name if sender else None,
        })
    return response


@router.get("/chat/{match_id}/project-invitations/options")
async def get_project_invitation_options(
    match_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    match = await _get_authorized_match(db, match_id, current_user)
    if match.owner_id != current_user.id:
        return {"can_invite": False, "reason": "Only the project owner can invite members", "open_role_slots": []}
    if match.is_member_added:
        return {"can_invite": False, "reason": "This match is already a project member", "open_role_slots": []}

    project = await db.get(Project, match.project_id)
    if not project:
        return {"can_invite": False, "reason": "Project not found", "open_role_slots": []}
    if project.review_status != "APPROVED":
        return {"can_invite": False, "reason": "Project is not approved yet", "project_id": project.id, "project_title": project.title, "open_role_slots": []}

    existing_member = await db.scalar(
        select(ProjectMember).where(
            ProjectMember.project_id == project.id,
            ProjectMember.user_id == match.user_id,
        )
    )
    if existing_member:
        return {"can_invite": False, "reason": "This user is already a member", "project_id": project.id, "project_title": project.title, "open_role_slots": []}

    role_slots = (await db.execute(
        select(ProjectRoleSlot).where(ProjectRoleSlot.project_id == project.id).order_by(ProjectRoleSlot.role_name)
    )).scalars().all()
    open_slots = [
        {"id": slot.id, "role_name": slot.role_name, "count": slot.count, "filled": slot.filled}
        for slot in role_slots
        if slot.filled < slot.count
    ]

    return {
        "can_invite": bool(open_slots),
        "reason": None if open_slots else "No open role slots remaining",
        "project_id": project.id,
        "project_title": project.title,
        "open_role_slots": open_slots,
    }


@router.get("/chat/{match_id}/project-invitations")
async def list_project_invitations(
    match_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    await _get_authorized_match(db, match_id, current_user)
    result = await db.execute(
        select(ProjectInvitation)
        .where(ProjectInvitation.match_id == match_id)
        .order_by(ProjectInvitation.created_at.desc())
    )
    return [await _build_invitation_response(db, invitation, current_user) for invitation in result.scalars().all()]


@router.post("/chat/{match_id}/project-invitations")
async def create_project_invitation(
    match_id: UUID,
    data: ProjectInvitationCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    match = await _get_authorized_match(db, match_id, current_user)
    if match.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only the project owner can invite members")
    if match.is_member_added:
        raise HTTPException(status_code=400, detail="This match is already a project member")

    slot = await db.get(ProjectRoleSlot, data.role_slot_id)
    if not slot or slot.project_id != match.project_id:
        raise HTTPException(status_code=404, detail="Role slot not found")
    if slot.filled >= slot.count:
        raise HTTPException(status_code=400, detail="Selected role slot is already filled")

    existing_pending = await db.scalar(
        select(ProjectInvitation).where(
            ProjectInvitation.match_id == match.id,
            ProjectInvitation.invitee_id == match.user_id,
            ProjectInvitation.status == "PENDING",
        )
    )
    if existing_pending:
        raise HTTPException(status_code=400, detail="There is already a pending invitation for this match")

    invitation = ProjectInvitation(
        match_id=match.id,
        project_id=match.project_id,
        inviter_id=current_user.id,
        invitee_id=match.user_id,
        role_slot_id=slot.id,
        role=slot.role_name,
        message=data.message,
    )
    db.add(invitation)
    await db.flush()

    project = await db.get(Project, match.project_id)
    db.add(Notification(
        user_id=match.user_id,
        type="PROJECT_INVITATION",
        title="Project invitation",
        body=f"{current_user.full_name} invited you to join {project.title if project else 'a project'} as {slot.role_name}.",
        related_id=str(invitation.id),
    ))

    response = await _build_invitation_response(db, invitation, current_user)
    await manager.send_message(str(match.user_id), str(match.id), {"type": "project_invitation", "data": response})
    return response


@router.post("/project-invitations/{invitation_id}/accept")
async def accept_project_invitation(
    invitation_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    invitation = await db.get(ProjectInvitation, invitation_id)
    if not invitation or invitation.invitee_id != current_user.id:
        raise HTTPException(status_code=404, detail="Invitation not found")
    if invitation.status != "PENDING":
        raise HTTPException(status_code=400, detail="Invitation is no longer pending")

    await _add_member_from_invitation(db, invitation)
    invitation.status = "ACCEPTED"
    invitation.responded_at = datetime.now(timezone.utc)

    db.add(Notification(
        user_id=invitation.inviter_id,
        type="PROJECT_INVITATION_ACCEPTED",
        title="Invitation accepted",
        body=f"{current_user.full_name} accepted your project invitation.",
        related_id=str(invitation.id),
    ))
    response = await _build_invitation_response(db, invitation, current_user)
    await manager.send_message(str(invitation.inviter_id), str(invitation.match_id), {"type": "project_invitation", "data": response})
    return response


@router.post("/project-invitations/{invitation_id}/decline")
async def decline_project_invitation(
    invitation_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    invitation = await db.get(ProjectInvitation, invitation_id)
    if not invitation or invitation.invitee_id != current_user.id:
        raise HTTPException(status_code=404, detail="Invitation not found")
    if invitation.status != "PENDING":
        raise HTTPException(status_code=400, detail="Invitation is no longer pending")

    invitation.status = "DECLINED"
    invitation.responded_at = datetime.now(timezone.utc)
    db.add(Notification(
        user_id=invitation.inviter_id,
        type="PROJECT_INVITATION_DECLINED",
        title="Invitation declined",
        body=f"{current_user.full_name} declined your project invitation.",
        related_id=str(invitation.id),
    ))
    response = await _build_invitation_response(db, invitation, current_user)
    await manager.send_message(str(invitation.inviter_id), str(invitation.match_id), {"type": "project_invitation", "data": response})
    return response


@router.get("/notifications")
async def list_notifications(
    limit: int = 20,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    notifications = await get_notifications(db, current_user.id, limit)
    return [
        {
            "id": str(n.id),
            "type": n.type,
            "title": n.title,
            "body": n.body,
            "is_read": n.is_read,
            "related_id": n.related_id,
            "created_at": n.created_at.isoformat(),
        }
        for n in notifications
    ]


@router.post("/notifications/{notification_id}/read")
async def mark_notification_read(
    notification_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    notif = await db.get(Notification, notification_id)
    if notif and notif.user_id == current_user.id:
        notif.is_read = True
    return {"message": "Notification marked as read"}


@router.get("/notifications/unread-count")
async def unread_count(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    count = await get_unread_notification_count(db, current_user.id)
    return {"count": count}
