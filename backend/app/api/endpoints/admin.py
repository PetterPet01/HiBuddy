from datetime import datetime, timezone
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, or_, func
from app.models.trust_safety import Report
from app.models.chat import Notification, RefreshToken
from app.models.operations import AdminAuditLog
from app.database import get_db
from app.core.dependencies import (
    STAFF_REVIEW_ROLES,
    get_current_admin,
    get_current_staff_reviewer,
    get_current_user,
)
from app.models.user import User
from app.models.project import Project
from app.schemas.admin import (
    AdminActionRequest,
    AdminRoleUpdateRequest,
    AdminUserResponse,
    AdminReportResponse,
    RejectStudentRequest,
    ResolveReportRequest,
    StaffOverviewResponse,
)

router = APIRouter(prefix="/api/v1/admin", tags=["admin"])


def _ensure_pending_student_verification(user: User) -> None:
    if user.verification_status != "PENDING":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Student verification request is not pending",
        )
    if not user.student_card_image_url:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Student verification request is missing student card evidence",
        )


def audit(admin: User, action: str, target_type: str, target_id: UUID, reason: str | None = None):
    return AdminAuditLog(
        admin_id=admin.id,
        action=action,
        target_type=target_type,
        target_id=str(target_id),
        reason=reason,
    )


def require_admin(current_user: User = Depends(get_current_user)):
    if current_user.role != "ADMIN":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin only"
        )
    return current_user


def require_staff_reviewer(current_user: User = Depends(get_current_staff_reviewer)):
    return current_user


@router.get("/student-verifications", response_model=list[AdminUserResponse])
async def list_student_verifications(
    search: str | None = None,
    offset: int = 0,
    limit: int = 50,
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.verification_status == "PENDING")
    if search:
        term = f"%{search.strip().lower()}%"
        stmt = stmt.where(
            or_(
                func.lower(User.full_name).like(term),
                func.lower(User.student_id).like(term),
                func.lower(User.university).like(term),
            )
        )
    stmt = (
        stmt.order_by(User.updated_at.desc())
        .offset(max(0, offset))
        .limit(min(max(limit, 1), 100))
    )

    result = await db.execute(stmt)
    return result.scalars().all()


@router.post("/student-verifications/{user_id}/approve", response_model=AdminUserResponse)
async def approve_student_verification(
    user_id: UUID,
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.id == user_id)
    user = (await db.execute(stmt)).scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    _ensure_pending_student_verification(user)

    user.verified_student = True
    user.verification_status = "APPROVED"
    user.verification_rejection_reason = None
    user.verification_reviewed_at = datetime.now(timezone.utc)
    user.verification_reviewed_by = current_user.id
    db.add(audit(current_user, "APPROVE_STUDENT", "USER", user.id, "Evidence reviewed"))
    db.add(
        Notification(
            user_id=user.id,
            type="STUDENT_VERIFICATION_APPROVED",
            title="Student verification approved",
            body="Your student verification was approved. Your profile now shows verified student status.",
            related_id=str(user.id),
        )
    )

    return user


@router.post("/student-verifications/{user_id}/reject", response_model=AdminUserResponse)
async def reject_student_verification(
    user_id: UUID,
    request: RejectStudentRequest,
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.id == user_id)
    user = (await db.execute(stmt)).scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    _ensure_pending_student_verification(user)

    user.verified_student = False
    user.verification_status = "REJECTED"
    user.verification_rejection_reason = request.reason
    user.verification_reviewed_at = datetime.now(timezone.utc)
    user.verification_reviewed_by = current_user.id
    db.add(audit(current_user, "REJECT_STUDENT", "USER", user.id, request.reason))
    db.add(
        Notification(
            user_id=user.id,
            type="STUDENT_VERIFICATION_REJECTED",
            title="Student verification needs changes",
            body=request.reason,
            related_id=str(user.id),
        )
    )

    return user


@router.post("/users/{user_id}/ban", response_model=AdminUserResponse)
async def ban_user(
    user_id: UUID,
    request: AdminActionRequest,
    current_user: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.id == user_id)
    user = (await db.execute(stmt)).scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    if user.role == "ADMIN":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot ban another admin"
        )

    user.is_active = False
    await db.execute(
        update(RefreshToken)
        .where(RefreshToken.user_id == user.id, RefreshToken.is_revoked.is_(False))
        .values(is_revoked=True)
    )
    db.add(audit(current_user, "BAN_USER", "USER", user.id, request.reason))
    from app.api.websocket import manager
    await manager.disconnect_user(str(user.id))

    return user


@router.post("/users/{user_id}/role", response_model=AdminUserResponse)
async def update_user_role(
    user_id: UUID,
    request: AdminRoleUpdateRequest,
    current_user: User = Depends(get_current_admin),
    db: AsyncSession = Depends(get_db),
):
    user = await db.get(User, user_id)
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
    if user.role == "ADMIN":
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot change another admin account")
    if user.id == current_user.id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot change your own role")

    previous_role = user.role
    user.role = request.role
    db.add(audit(current_user, "UPDATE_USER_ROLE", "USER", user.id, request.reason))
    db.add(
        Notification(
            user_id=user.id,
            type="STAFF_ROLE_UPDATED",
            title="Account role updated",
            body=f"Your role changed from {previous_role} to {request.role}.",
            related_id=str(user.id),
        )
    )
    return user


@router.post("/users/{user_id}/unban", response_model=AdminUserResponse)
async def unban_user(
    user_id: UUID,
    request: AdminActionRequest,
    current_user: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.id == user_id)
    user = (await db.execute(stmt)).scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    user.is_active = True
    db.add(audit(current_user, "UNBAN_USER", "USER", user.id, request.reason))

    return user

@router.get("/users", response_model=list[AdminUserResponse])
async def list_users(
    search: str | None = None,
    active: bool | None = None,
    offset: int = 0,
    limit: int = 50,
    current_user: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(User).where(User.role != "ADMIN")
    if search:
        term = f"%{search.strip().lower()}%"
        stmt = stmt.where(
            or_(
                func.lower(User.full_name).like(term),
                func.lower(User.username).like(term),
                func.lower(User.email).like(term),
            )
        )
    if active is not None:
        stmt = stmt.where(User.is_active == active)
    stmt = (
        stmt.order_by(User.created_at.desc())
        .offset(max(0, offset))
        .limit(min(max(limit, 1), 100))
    )

    result = await db.execute(stmt)
    return result.scalars().all()


@router.get("/overview", response_model=StaffOverviewResponse)
async def get_staff_overview(
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db),
):
    total_users = await db.scalar(select(func.count()).select_from(User).where(User.role == "MEMBER"))
    active_users = await db.scalar(
        select(func.count()).select_from(User).where(User.role == "MEMBER", User.is_active.is_(True))
    )
    banned_users = await db.scalar(
        select(func.count()).select_from(User).where(User.role == "MEMBER", User.is_active.is_(False))
    )
    verified_students = await db.scalar(
        select(func.count()).select_from(User).where(User.role == "MEMBER", User.verified_student.is_(True))
    )
    pending_verifications = await db.scalar(
        select(func.count()).select_from(User).where(User.verification_status == "PENDING")
    )
    open_reports = await db.scalar(
        select(func.count()).select_from(Report).where(Report.status == "PENDING")
    )
    flagged_projects = await db.scalar(
        select(func.count()).select_from(Project).where(Project.review_status == "FLAGGED")
    )
    admin_users = await db.scalar(
        select(func.count()).select_from(User).where(User.role == "ADMIN")
    )
    moderator_users = await db.scalar(
        select(func.count()).select_from(User).where(User.role == "MODERATOR")
    )

    return StaffOverviewResponse(
        total_users=total_users or 0,
        active_users=active_users or 0,
        banned_users=banned_users or 0,
        verified_students=verified_students or 0,
        pending_verifications=pending_verifications or 0,
        open_reports=open_reports or 0,
        flagged_projects=flagged_projects or 0,
        admin_users=admin_users or 0,
        moderator_users=moderator_users or 0,
    )


@router.get("/reports", response_model=list[AdminReportResponse])
async def list_reports(
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(Report)
        .where(Report.status == "PENDING")
        .order_by(Report.created_at.desc())
    )

    result = await db.execute(stmt)
    reports = result.scalars().all()

    response = []

    for report in reports:
        reporter = await db.get(User, report.reporter_id)
        reported = await db.get(User, report.reported_id)

        response.append(
            AdminReportResponse(
                id=report.id,
                reporter_id=report.reporter_id,
                reported_id=report.reported_id,
                reason=report.reason,
                description=report.description,
                evidence_url=report.evidence_url,
                context_type=report.context_type,
                context_id=report.context_id,
                status=report.status,
                created_at=report.created_at,
                reporter_name=reporter.full_name if reporter else None,
                reported_name=reported.full_name if reported else None,
            )
        )

    return response

@router.post("/reports/{report_id}/resolve", response_model=AdminReportResponse)
async def resolve_report(
    report_id: UUID,
    request: ResolveReportRequest,
    current_user: User = Depends(require_staff_reviewer),
    db: AsyncSession = Depends(get_db)
):
    report = await db.get(Report, report_id)

    if not report:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Report not found"
        )

    reported_user = await db.get(User, report.reported_id)

    if request.action == "BAN":
        if current_user.role != "ADMIN":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Only admins can ban users from report review",
            )
        if reported_user:
            if reported_user.role in STAFF_REVIEW_ROLES:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Cannot ban staff reviewer"
                )
            reported_user.is_active = False
            await db.execute(
                update(RefreshToken)
                .where(RefreshToken.user_id == reported_user.id)
                .values(is_revoked=True)
            )
            from app.api.websocket import manager
            await manager.disconnect_user(str(reported_user.id))

        report.status = "RESOLVED_BANNED"

    elif request.action == "DISMISS":
        report.status = "DISMISSED"

    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid action"
        )

    db.add(
        audit(
            current_user,
            f"RESOLVE_REPORT_{request.action}",
            "REPORT",
            report.id,
            request.reason,
        )
    )

    reporter = await db.get(User, report.reporter_id)
    reported = await db.get(User, report.reported_id)

    return AdminReportResponse(
        id=report.id,
        reporter_id=report.reporter_id,
        reported_id=report.reported_id,
        reason=report.reason,
        description=report.description,
        evidence_url=report.evidence_url,
        context_type=report.context_type,
        context_id=report.context_id,
        status=report.status,
        created_at=report.created_at,
        reporter_name=reporter.full_name if reporter else None,
        reported_name=reported.full_name if reported else None,
    )
