from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, or_, and_
from datetime import datetime, timedelta, timezone
from typing import List

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.trust_safety import UserBlock, Report
from app.models.swipe import Match
from app.schemas.trust_safety import UserBlockCreate, UserBlockResponse, ReportCreate, ReportResponse

router = APIRouter(prefix="/api/v1/trust", tags=["trust-safety"])

@router.post("/block", response_model=UserBlockResponse, status_code=status.HTTP_201_CREATED)
async def block_user(
    data: UserBlockCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    if data.blocked_id == current_user.id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="You cannot block yourself.")
    blocked_user = await db.get(User, data.blocked_id)
    if not blocked_user:
        raise HTTPException(status_code=404, detail="User not found")

    # Check if already blocked
    stmt = select(UserBlock).where(
        UserBlock.blocker_id == current_user.id,
        UserBlock.blocked_id == data.blocked_id
    )
    existing_block = (await db.execute(stmt)).scalar_one_or_none()

    if existing_block:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="User is already blocked.")

    block = UserBlock(
        blocker_id=current_user.id,
        blocked_id=data.blocked_id,
        reason=data.reason
    )
    db.add(block)
    matches = (
        await db.execute(
            select(Match).where(
                Match.is_unmatched == False,
                or_(
                    and_(Match.user_id == current_user.id, Match.owner_id == data.blocked_id),
                    and_(Match.user_id == data.blocked_id, Match.owner_id == current_user.id),
                ),
            )
        )
    ).scalars().all()
    for match in matches:
        match.is_unmatched = True
        match.unmatched_at = datetime.now(timezone.utc)
    await db.flush()
    return block

@router.delete("/block/{blocked_id}", status_code=status.HTTP_204_NO_CONTENT)
async def unblock_user(
    blocked_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(UserBlock).where(
        UserBlock.blocker_id == current_user.id,
        UserBlock.blocked_id == blocked_id
    )
    block = (await db.execute(stmt)).scalar_one_or_none()

    if not block:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Block record not found.")

    await db.delete(block)
    await db.commit()
    return None

@router.get("/blocks", response_model=List[UserBlockResponse])
async def get_blocked_users(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(UserBlock).where(UserBlock.blocker_id == current_user.id)
    blocks = (await db.execute(stmt)).scalars().all()
    return list(blocks)

@router.post("/report", response_model=ReportResponse, status_code=status.HTTP_201_CREATED)
async def report_user(
    data: ReportCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    if data.reported_id == current_user.id:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="You cannot report yourself.")
    if not await db.get(User, data.reported_id):
        raise HTTPException(status_code=404, detail="User not found")
    recent = await db.scalar(
        select(Report.id).where(
            Report.reporter_id == current_user.id,
            Report.reported_id == data.reported_id,
            Report.created_at >= datetime.now(timezone.utc) - timedelta(hours=24),
        )
    )
    if recent:
        raise HTTPException(status_code=429, detail="You already reported this user recently")

    report = Report(
        reporter_id=current_user.id,
        reported_id=data.reported_id,
        reason=data.reason,
        description=data.description,
        evidence_url=data.evidence_url,
        context_type=data.context_type,
        context_id=data.context_id,
        status="PENDING"
    )
    db.add(report)
    await db.flush()
    return report
