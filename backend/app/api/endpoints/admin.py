from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.schemas.admin import AdminUserResponse, RejectStudentRequest

router = APIRouter(prefix="/api/v1/admin", tags=["admin"])


def require_admin(current_user: User = Depends(get_current_user)):
    if current_user.role != "ADMIN":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin only"
        )
    return current_user


@router.get("/student-verifications", response_model=list[AdminUserResponse])
async def list_student_verifications(
    current_user: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(User)
        .where(User.verification_status == "PENDING")
        .order_by(User.updated_at.desc())
    )

    result = await db.execute(stmt)
    return result.scalars().all()


@router.post("/student-verifications/{user_id}/approve", response_model=AdminUserResponse)
async def approve_student_verification(
    user_id: UUID,
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

    user.verified_student = True
    user.verification_status = "APPROVED"
    user.verification_rejection_reason = None

    await db.commit()
    await db.refresh(user)

    return user


@router.post("/student-verifications/{user_id}/reject", response_model=AdminUserResponse)
async def reject_student_verification(
    user_id: UUID,
    request: RejectStudentRequest,
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

    user.verified_student = False
    user.verification_status = "REJECTED"
    user.verification_rejection_reason = request.reason

    await db.commit()
    await db.refresh(user)

    return user


@router.post("/users/{user_id}/ban", response_model=AdminUserResponse)
async def ban_user(
    user_id: UUID,
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

    await db.commit()
    await db.refresh(user)

    return user


@router.post("/users/{user_id}/unban", response_model=AdminUserResponse)
async def unban_user(
    user_id: UUID,
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

    await db.commit()
    await db.refresh(user)

    return user

@router.get("/users", response_model=list[AdminUserResponse])
async def list_users(
    current_user: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db)
):
    stmt = (
        select(User)
        .where(User.role != "ADMIN")
        .order_by(User.created_at.desc())
    )

    result = await db.execute(stmt)
    return result.scalars().all()