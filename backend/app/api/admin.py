import logging
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.core.dependencies import get_current_admin
from app.models.user import User
from app.models.project import Project, ProjectMember
from app.models.chat import Notification
from app.models.operations import AdminAuditLog, OutboxEvent
from app.schemas.admin import AdminActionRequest
from app.schemas.project import ProjectResponse, RoleSlotResponse, ProjectMemberResponse
from app.services.embedding_service import upsert_project_vector
from app.services.fcm_service import notify_user
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter(prefix="/api/v1/admin", tags=["admin"])


async def _build_project_response(db: AsyncSession, project: Project) -> ProjectResponse:
    from app.models.project import ProjectRoleSlot
    slots_result = await db.execute(
        select(ProjectRoleSlot).where(ProjectRoleSlot.project_id == project.id)
    )
    members_result = await db.execute(
        select(ProjectMember).where(ProjectMember.project_id == project.id)
    )
    members = members_result.scalars().all()

    member_responses = []
    for m in members:
        member_user = await db.get(User, m.user_id)
        member_responses.append(ProjectMemberResponse(
            id=m.id,
            user_id=m.user_id,
            display_name=member_user.full_name if member_user else "",
            role=m.role,
            is_owner=m.is_owner,
            avatar_url=member_user.avatar_url if member_user else None,
            joined_at=m.joined_at,
        ))

    return ProjectResponse(
        id=project.id,
        owner_id=project.owner_id,
        title=project.title,
        field=project.field,
        description=project.description,
        specific_goal=project.specific_goal,
        thumbnail_url=project.thumbnail_url,
        work_mode=project.work_mode,
        commitment_level=project.commitment_level,
        start_date=project.start_date,
        end_date=project.end_date,
        max_members=project.max_members,
        status=project.status,
        review_status=project.review_status,
        moderation_categories=project.moderation_categories,
        moderation_reasons=project.moderation_reasons,
        additional_requirements=project.additional_requirements,
        member_benefits=project.member_benefits,
        role_slots=[RoleSlotResponse.model_validate(s) for s in slots_result.scalars().all()],
        members=member_responses,
        created_at=project.created_at,
    )


@router.get("/projects/flagged", response_model=list[ProjectResponse])
async def list_flagged_projects(
    current_admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Project)
        .options(selectinload(Project.role_slots))
        .where(Project.review_status == "FLAGGED")
        .order_by(Project.created_at.desc())
    )
    projects = result.scalars().all()

    response = []
    for project in projects:
        response.append(await _build_project_response(db, project))

    return response


@router.post("/projects/{project_id}/approve", response_model=ProjectResponse)
async def approve_project(
    project_id: UUID,
    request: AdminActionRequest,
    current_admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(
        Project, project_id,
        options=[selectinload(Project.role_slots)],
    )
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    if project.review_status != "FLAGGED":
        raise HTTPException(status_code=400, detail="Project is not in flagged status")

    project.review_status = "APPROVED"

    notif = Notification(
        user_id=project.owner_id,
        type="PROJECT_APPROVED",
        title="Dự án của bạn đã được phê duyệt",
        body=f"Dự án '{project.title}' đã được phê duyệt và hiện đang được đăng tải.",
        related_id=str(project.id),
    )
    db.add(notif)

    db.add(AdminAuditLog(
        admin_id=current_admin.id,
        action="APPROVE_PROJECT",
        target_type="PROJECT",
        target_id=str(project.id),
        reason=request.reason,
    ))
    db.add(OutboxEvent(
        event_type="PUSH_NOTIFICATION",
        payload={
            "user_id": str(project.owner_id),
            "title": "Project approved",
            "body": f"Your project '{project.title}' is now visible.",
        },
    ))

    return await _build_project_response(db, project)


@router.post("/projects/{project_id}/reject", response_model=ProjectResponse)
async def reject_project(
    project_id: UUID,
    request: AdminActionRequest,
    current_admin: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    if project.review_status != "FLAGGED":
        raise HTTPException(status_code=400, detail="Project is not in flagged status")

    project.review_status = "REJECTED"
    project.status = "CLOSED"

    notif = Notification(
        user_id=project.owner_id,
        type="PROJECT_REJECTED",
        title="Dự án của bạn không được phê duyệt",
        body=f"Dự án '{project.title}' đã bị từ chối vì vi phạm tiêu chuẩn cộng đồng.",
        related_id=str(project.id),
    )
    db.add(notif)

    db.add(AdminAuditLog(
        admin_id=current_admin.id,
        action="REJECT_PROJECT",
        target_type="PROJECT",
        target_id=str(project.id),
        reason=request.reason,
    ))
    db.add(OutboxEvent(
        event_type="PUSH_NOTIFICATION",
        payload={
            "user_id": str(project.owner_id),
            "title": "Project rejected",
            "body": request.reason,
        },
    ))

    return await _build_project_response(db, project)
