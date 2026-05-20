from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from datetime import datetime, timezone

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.fcm_token import FCMToken
from app.schemas.fcm_token import FCMTokenRegister, FCMTokenResponse

router = APIRouter(prefix="/api/v1/fcm", tags=["fcm"])

@router.post("/register", response_model=FCMTokenResponse)
async def register_token(
    data: FCMTokenRegister,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Check if token already exists for any user and update/reassign it
    stmt = select(FCMToken).where(FCMToken.token == data.token)
    existing_token = (await db.execute(stmt)).scalar_one_or_none()

    if existing_token:
        if existing_token.user_id != current_user.id:
            # Reassign to current user
            existing_token.user_id = current_user.id
            existing_token.device_type = data.device_type

        # Update last_used_at
        existing_token.last_used_at = func.now()
        existing_token.is_active = True

        await db.commit()
        await db.refresh(existing_token)
        return existing_token

    # Create new token
    new_token = FCMToken(
        user_id=current_user.id,
        token=data.token,
        device_type=data.device_type,
        is_active=True
    )
    db.add(new_token)
    await db.commit()
    await db.refresh(new_token)
    return new_token

@router.delete("/unregister/{token}", status_code=status.HTTP_204_NO_CONTENT)
async def unregister_token(
    token: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    stmt = select(FCMToken).where(
        FCMToken.token == token,
        FCMToken.user_id == current_user.id
    )
    fcm_token = (await db.execute(stmt)).scalar_one_or_none()

    if not fcm_token:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Token not found.")

    await db.delete(fcm_token)
    await db.commit()
    return None
