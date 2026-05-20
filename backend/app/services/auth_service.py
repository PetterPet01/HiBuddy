import random
import string
from datetime import datetime, timedelta, timezone
from uuid import UUID

from fastapi import HTTPException, status, BackgroundTasks
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.core.security import verify_password, get_password_hash, create_access_token, create_refresh_token, decode_token
from app.models.user import User
from app.models.profile import UserProfile
from app.models.chat import RefreshToken
from app.schemas.auth import (
    UserRegister, UserLogin, UserResponse, TokenResponse,
    TokenRefresh, ForgotPassword, ResetPassword, EmailVerifyRequest,
    StudentVerificationRequest,
)
from app.config import get_settings

settings = get_settings()


def generate_code(length: int = 6) -> str:
    return "".join(random.choices(string.digits, k=length))


async def register_user(db: AsyncSession, data: UserRegister, background_tasks: BackgroundTasks) -> TokenResponse:
    existing = await db.execute(
        select(User).where((User.email == str(data.email)) | (User.username == data.username))
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email or username already registered")

    if data.password != data.confirm_password:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Passwords do not match")

    dob = datetime.strptime(data.date_of_birth, "%d/%m/%Y").replace(tzinfo=timezone.utc)

    verify_code = generate_code()
    user = User(
        username=data.username,
        email=str(data.email),
        full_name=data.full_name,
        hashed_password=get_password_hash(data.password),
        phone=data.phone,
        date_of_birth=dob,
        email_verify_code=verify_code,
        email_verify_code_expires=datetime.now(timezone.utc) + timedelta(hours=24),
        role="MEMBER",
    )
    db.add(user)
    await db.flush()

    profile = UserProfile(user_id=user.id, display_name=data.full_name)
    db.add(profile)

    await db.flush()

    background_tasks.add_task(send_verification_email, str(data.email), verify_code)

    access_token = create_access_token(str(user.id))
    refresh_token_delta = timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    refresh_token = create_refresh_token(str(user.id), expires_delta=refresh_token_delta)

    rt = RefreshToken(
        user_id=user.id,
        token_hash=get_password_hash(refresh_token),
        expires_at=datetime.now(timezone.utc) + refresh_token_delta,
    )
    db.add(rt)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
    )


async def login_user(db: AsyncSession, data: UserLogin) -> TokenResponse:
    user_result = await db.execute(
        select(User).where((User.username == data.username) | (User.email == data.username))
    )
    user = user_result.scalar_one_or_none()

    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

    if user.locked_until and user.locked_until > datetime.now(timezone.utc):
        minutes_left = int((user.locked_until - datetime.now(timezone.utc)).total_seconds() / 60)
        raise HTTPException(
            status_code=status.HTTP_423_LOCKED,
            detail=f"Account locked. Try again in {minutes_left} minutes",
        )

    if not verify_password(data.password, user.hashed_password):
        user.login_attempts += 1
        if user.login_attempts >= 5:
            user.locked_until = datetime.now(timezone.utc) + timedelta(minutes=15)
        await db.flush()
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")

    user.login_attempts = 0
    user.locked_until = None
    user.last_login = datetime.now(timezone.utc)

    access_token = create_access_token(str(user.id))
    refresh_token_delta = timedelta(days=30 if data.remember_me else settings.REFRESH_TOKEN_EXPIRE_DAYS)
    refresh_token = create_refresh_token(str(user.id), expires_delta=refresh_token_delta)

    rt = RefreshToken(
        user_id=user.id,
        token_hash=get_password_hash(refresh_token),
        expires_at=datetime.now(timezone.utc) + refresh_token_delta,
    )
    db.add(rt)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
    )


async def refresh_access_token(db: AsyncSession, data: TokenRefresh) -> TokenResponse:
    payload = decode_token(data.refresh_token)
    if not payload or payload.get("type") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")

    user_id = payload.get("sub")
    try:
        user_uuid = UUID(user_id)
    except (TypeError, ValueError):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")

    user_result = await db.execute(select(User).where(User.id == user_uuid))
    user = user_result.scalar_one_or_none()
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    token_result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.user_id == user.id,
            RefreshToken.is_revoked == False,
            RefreshToken.expires_at > datetime.now(timezone.utc),
        ).order_by(RefreshToken.created_at.desc())
    )
    stored_tokens = token_result.scalars().all()
    if not stored_tokens:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token expired")

    stored_token = next(
        (token for token in stored_tokens if verify_password(data.refresh_token, token.token_hash)),
        None,
    )
    if not stored_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token")

    new_access = create_access_token(str(user.id))

    return TokenResponse(
        access_token=new_access,
        refresh_token=data.refresh_token,
        user=UserResponse.model_validate(user),
    )


async def logout_user(db: AsyncSession, user: User) -> None:
    result = await db.execute(
        select(RefreshToken).where(RefreshToken.user_id == user.id, RefreshToken.is_revoked == False)
    )
    for token in result.scalars():
        token.is_revoked = True


async def verify_email(db: AsyncSession, data: EmailVerifyRequest) -> dict:
    user = await _get_user_by_verify_code(db, data.code)
    if not user:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid or expired verification code")
    if user.email_verify_code_expires and user.email_verify_code_expires < datetime.now(timezone.utc):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Verification code expired")
    user.email_verified = True
    user.email_verify_code = None
    user.email_verify_code_expires = None
    return {"message": "Email verified successfully"}


async def resend_verification(db: AsyncSession, user: User, background_tasks: BackgroundTasks) -> dict:
    if user.email_verified:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email already verified")

    user.email_verify_code = generate_code()
    user.email_verify_code_expires = datetime.now(timezone.utc) + timedelta(hours=24)
    background_tasks.add_task(send_verification_email, user.email, user.email_verify_code)
    return {"message": "Verification email sent"}


async def forgot_password(db: AsyncSession, data: ForgotPassword, background_tasks: BackgroundTasks) -> dict:
    if data.email:
        result = await db.execute(select(User).where(User.email == data.email))
    elif data.phone:
        result = await db.execute(select(User).where(User.phone == data.phone))
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Email or phone required")

    user = result.scalar_one_or_none()
    if not user:
        return {"message": "If the account exists, a reset code has been sent"}

    code = generate_code()
    user.email_verify_code = code
    user.email_verify_code_expires = datetime.now(timezone.utc) + timedelta(minutes=30)
    background_tasks.add_task(send_reset_code_email, user.email, code)
    return {"message": "If the account exists, a reset code has been sent"}


async def reset_password(db: AsyncSession, data: ResetPassword) -> dict:
    user = await _get_user_by_verify_code(db, data.code)
    if not user:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid or expired reset code")
    if data.new_password != data.confirm_password:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Passwords do not match")

    user.hashed_password = get_password_hash(data.new_password)
    user.email_verify_code = None
    user.email_verify_code_expires = None
    user.login_attempts = 0
    user.locked_until = None

    result = await db.execute(select(RefreshToken).where(RefreshToken.user_id == user.id))
    for token in result.scalars():
        token.is_revoked = True

    return {"message": "Password reset successfully"}


async def submit_student_verification(
    db: AsyncSession, user: User, data: StudentVerificationRequest
) -> dict:
    if data.student_email and not data.student_email.endswith(".edu.vn"):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Student email must end with .edu.vn")

    existing = await db.execute(
        select(User).where(User.student_id == data.student_id, User.id != user.id)
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Student ID already linked to another account")

    user.student_email = data.student_email or user.student_email
    user.university = data.university
    user.student_id = data.student_id
    user.verification_status = "PENDING"

    return {"message": "Student verification submitted for review"}


async def _get_user_by_verify_code(db: AsyncSession, code: str) -> User | None:
    result = await db.execute(select(User).where(User.email_verify_code == code))
    return result.scalar_one_or_none()


async def send_verification_email(email: str, code: str):
    pass


async def send_reset_code_email(email: str, code: str):
    pass
