import logging

import asyncio
from datetime import datetime, timedelta, timezone
from email.message import EmailMessage
from uuid import UUID, uuid4

import aiosmtplib
from fastapi import BackgroundTasks, HTTPException, status
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    generate_numeric_code,
    get_password_hash,
    hash_token,
    verify_password,
)
from app.models.auth import AccountToken, AuthIdentity
from app.models.chat import RefreshToken
from app.models.profile import UserProfile
from app.models.user import User
from app.schemas.auth import (
    EmailVerifyRequest,
    ForgotPassword,
    GoogleLoginRequest,
    ResetPassword,
    StudentVerificationRequest,
    TokenRefresh,
    TokenResponse,
    UserLogin,
    UserRegister,
    UserResponse,
)

settings = get_settings()
EMAIL_VERIFICATION = "EMAIL_VERIFICATION"
PASSWORD_RESET = "PASSWORD_RESET"


async def _issue_session(
    db: AsyncSession,
    user: User,
    *,
    remember_me: bool,
    device_name: str | None = None,
    family: str | None = None,
) -> TokenResponse:
    lifetime_days = settings.REFRESH_TOKEN_EXPIRE_DAYS if remember_me else settings.REFRESH_TOKEN_SHORT_DAYS
    lifetime = timedelta(days=lifetime_days)
    jti = str(uuid4())
    family = family or str(uuid4())
    refresh_token = create_refresh_token(
        str(user.id), expires_delta=lifetime, jti=jti, family=family
    )
    jti_hash = hash_token(jti)
    db.add(
        RefreshToken(
            user_id=user.id,
            token_hash=hash_token(refresh_token),
            jti_hash=jti_hash,
            token_family=family,
            device_name=(device_name or "Unknown device")[:120],
            expires_at=datetime.now(timezone.utc) + lifetime,
        )
    )
    return TokenResponse(
        access_token=create_access_token(str(user.id)),
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
        requires_email_verification=not user.email_verified,
    )


async def _create_account_code(
    db: AsyncSession,
    user: User,
    purpose: str,
    *,
    enforce_cooldown: bool,
) -> str:
    now = datetime.now(timezone.utc)
    latest_result = await db.execute(
        select(AccountToken)
        .where(
            AccountToken.user_id == user.id,
            AccountToken.purpose == purpose,
            AccountToken.consumed_at.is_(None),
        )
        .order_by(AccountToken.created_at.desc())
        .limit(1)
    )
    latest = latest_result.scalar_one_or_none()
    if latest and enforce_cooldown:
        next_send = latest.last_sent_at + timedelta(seconds=settings.AUTH_CODE_RESEND_SECONDS)
        if next_send > now:
            retry_after = max(1, int((next_send - now).total_seconds()))
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail=f"Please wait {retry_after} seconds before requesting another code",
                headers={"Retry-After": str(retry_after)},
            )
    if latest:
        latest.consumed_at = now

    code = generate_numeric_code()
    db.add(
        AccountToken(
            user_id=user.id,
            purpose=purpose,
            code_hash=hash_token(code),
            expires_at=now + timedelta(minutes=settings.AUTH_CODE_TTL_MINUTES),
            last_sent_at=now,
        )
    )
    return code


async def _consume_account_code(
    db: AsyncSession,
    *,
    email: str,
    purpose: str,
    code: str,
) -> User:
    result = await db.execute(
        select(AccountToken, User)
        .join(User, User.id == AccountToken.user_id)
        .where(
            func.lower(User.email) == email.strip().lower(),
            AccountToken.purpose == purpose,
            AccountToken.consumed_at.is_(None),
        )
        .order_by(AccountToken.created_at.desc())
        .limit(1)
    )
    row = result.first()
    if not row:
        raise HTTPException(status_code=400, detail="Invalid or expired code")

    token, user = row
    now = datetime.now(timezone.utc)
    if token.expires_at <= now:
        token.consumed_at = now
        await db.commit()
        raise HTTPException(status_code=400, detail="Invalid or expired code")
    if token.attempts >= settings.AUTH_CODE_MAX_ATTEMPTS:
        token.consumed_at = now
        await db.commit()
        raise HTTPException(status_code=429, detail="Too many invalid code attempts")
    if token.code_hash != hash_token(code.strip()):
        token.attempts += 1
        if token.attempts >= settings.AUTH_CODE_MAX_ATTEMPTS:
            token.consumed_at = now
        await db.commit()
        raise HTTPException(status_code=400, detail="Invalid or expired code")

    token.consumed_at = now
    return user


async def register_user(
    db: AsyncSession, data: UserRegister, background_tasks: BackgroundTasks
) -> TokenResponse:
    email = str(data.email).strip().lower()
    username = data.username.strip().lower()
    existing_email = await db.execute(
        select(User.id).where(func.lower(User.email) == email).limit(1)
    )
    if existing_email.scalar() is not None:
        raise HTTPException(status_code=409, detail="Email already registered")

    existing_username = await db.execute(
        select(User.id).where(func.lower(User.username) == username).limit(1)
    )
    if existing_username.scalar() is not None:
        raise HTTPException(status_code=409, detail="Username already registered")

    dob = datetime.strptime(data.date_of_birth, "%d/%m/%Y").replace(tzinfo=timezone.utc)
    user = User(
        username=username,
        email=email,
        full_name=data.full_name.strip(),
        hashed_password=get_password_hash(data.password),
        phone=data.phone.strip() if data.phone else None,
        date_of_birth=dob,
        role="MEMBER",
    )
    db.add(user)
    try:
        await db.flush()
    except IntegrityError as exc:
        raise HTTPException(status_code=409, detail="Email or username already registered") from exc
    db.add(UserProfile(user_id=user.id, display_name=user.full_name))
    code = await _create_account_code(db, user, EMAIL_VERIFICATION, enforce_cooldown=False)
    background_tasks.add_task(send_verification_email, user.email, code)
    return await _issue_session(db, user, remember_me=False, device_name="Registration")


async def login_user(db: AsyncSession, data: UserLogin) -> TokenResponse:
    identifier = data.username.strip().lower()
    result = await db.execute(
        select(User)
        .where(
            (func.lower(User.username) == identifier) | (func.lower(User.email) == identifier)
        )
        .order_by(User.created_at.asc(), User.id.asc())
        .limit(1)
    )
    user = result.scalar_one_or_none()
    if not user or not user.hashed_password:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    now = datetime.now(timezone.utc)
    if user.locked_until and user.locked_until > now:
        seconds = int((user.locked_until - now).total_seconds())
        raise HTTPException(
            status_code=423,
            detail="Account temporarily locked",
            headers={"Retry-After": str(max(1, seconds))},
        )
    if not verify_password(data.password, user.hashed_password):
        user.login_attempts += 1
        if user.login_attempts >= 5:
            user.locked_until = now + timedelta(minutes=15)
        await db.commit()
        raise HTTPException(status_code=401, detail="Invalid credentials")
    if not user.is_active:
        raise HTTPException(status_code=403, detail="Account has been banned")
    user.login_attempts = 0
    user.locked_until = None
    user.last_login = now
    return await _issue_session(
        db, user, remember_me=data.remember_me, device_name=data.device_name
    )


async def google_login(db: AsyncSession, data: GoogleLoginRequest) -> TokenResponse:
    if not settings.GOOGLE_CLIENT_ID:
        raise HTTPException(status_code=503, detail="Google sign-in is not configured")
    try:
        claims = await asyncio.to_thread(
            google_id_token.verify_oauth2_token,
            data.id_token,
            google_requests.Request(),
            settings.GOOGLE_CLIENT_ID,
        )
    except Exception as exc:
        raise HTTPException(status_code=401, detail="Invalid Google identity token") from exc
    if not claims.get("email_verified"):
        raise HTTPException(status_code=403, detail="Google email is not verified")

    subject = str(claims["sub"])
    email = str(claims["email"]).lower()
    identity_result = await db.execute(
        select(AuthIdentity).where(
            AuthIdentity.provider == "GOOGLE",
            AuthIdentity.provider_subject == subject,
        )
    )
    identity = identity_result.scalar_one_or_none()
    user = await db.get(User, identity.user_id) if identity else None
    if not user:
        user_result = await db.execute(
            select(User)
            .where(func.lower(User.email) == email)
            .order_by(User.created_at.asc(), User.id.asc())
            .limit(1)
        )
        user = user_result.scalar_one_or_none()
    if not user:
        username_base = email.split("@", 1)[0][:40]
        username = username_base
        suffix = 1
        while (
            await db.execute(select(User.id).where(User.username == username))
        ).scalar_one_or_none():
            suffix += 1
            username = f"{username_base[:35]}{suffix}"
        user = User(
            username=username,
            email=email,
            full_name=str(claims.get("name") or username),
            hashed_password=None,
            avatar_url=claims.get("picture"),
            email_verified=True,
            role="MEMBER",
        )
        db.add(user)
        await db.flush()
        db.add(UserProfile(user_id=user.id, display_name=user.full_name))
    if not identity:
        db.add(
            AuthIdentity(
                user_id=user.id,
                provider="GOOGLE",
                provider_subject=subject,
                provider_email=email,
            )
        )
    if not user.is_active:
        raise HTTPException(status_code=403, detail="Account has been banned")
    user.email_verified = True
    user.last_login = datetime.now(timezone.utc)
    return await _issue_session(db, user, remember_me=True, device_name=data.device_name)


async def refresh_access_token(db: AsyncSession, data: TokenRefresh) -> TokenResponse:
    payload = decode_token(data.refresh_token)
    if not payload or payload.get("type") != "refresh":
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    try:
        user_id = UUID(payload["sub"])
    except (KeyError, TypeError, ValueError) as exc:
        raise HTTPException(status_code=401, detail="Invalid refresh token") from exc
    jti_hash = hash_token(str(payload.get("jti", "")))
    token_result = await db.execute(
        select(RefreshToken).where(RefreshToken.jti_hash == jti_hash)
    )
    stored = token_result.scalar_one_or_none()
    now = datetime.now(timezone.utc)
    if not stored:
        family = payload.get("family")
        if family:
            await db.execute(
                update(RefreshToken)
                .where(RefreshToken.token_family == family)
                .values(is_revoked=True, revoked_at=now)
            )
        raise HTTPException(status_code=401, detail="Refresh token reuse detected")
    if stored.is_revoked or stored.expires_at <= now:
        raise HTTPException(status_code=401, detail="Refresh token expired")
    if stored.token_hash != hash_token(data.refresh_token):
        raise HTTPException(status_code=401, detail="Invalid refresh token")

    user = await db.get(User, user_id)
    if not user or not user.is_active:
        raise HTTPException(status_code=401, detail="Account is unavailable")
    stored.is_revoked = True
    stored.revoked_at = now
    response = await _issue_session(
        db,
        user,
        remember_me=(stored.expires_at - stored.created_at).days > settings.REFRESH_TOKEN_SHORT_DAYS,
        device_name=data.device_name or stored.device_name,
        family=stored.token_family,
    )
    new_payload = decode_token(response.refresh_token)
    stored.replaced_by_jti_hash = hash_token(str(new_payload["jti"]))
    return response


async def logout_user(
    db: AsyncSession, user: User, refresh_token: str | None = None
) -> dict:
    now = datetime.now(timezone.utc)
    if refresh_token:
        payload = decode_token(refresh_token)
        jti = payload.get("jti") if payload else None
        if jti:
            await db.execute(
                update(RefreshToken)
                .where(
                    RefreshToken.user_id == user.id,
                    RefreshToken.jti_hash == hash_token(str(jti)),
                )
                .values(is_revoked=True, revoked_at=now)
            )
    else:
        await db.execute(
            update(RefreshToken)
            .where(RefreshToken.user_id == user.id, RefreshToken.is_revoked.is_(False))
            .values(is_revoked=True, revoked_at=now)
        )
    return {"message": "Signed out"}


async def verify_email(
    db: AsyncSession, current_user: User, data: EmailVerifyRequest
) -> dict:
    if str(data.email).strip().lower() != current_user.email.strip().lower():
        raise HTTPException(
            status_code=403,
            detail="Verification email does not match the signed-in account",
        )
    if current_user.email_verified:
        return {"message": "Email already verified"}

    user = await _consume_account_code(
        db, email=current_user.email, purpose=EMAIL_VERIFICATION, code=data.code
    )
    if user.id != current_user.id:
        raise HTTPException(status_code=403, detail="Verification code does not match this account")
    user.email_verified = True
    return {"message": "Email verified successfully"}


async def resend_verification(
    db: AsyncSession, user: User, background_tasks: BackgroundTasks
) -> dict:
    if user.email_verified:
        raise HTTPException(status_code=400, detail="Email already verified")
    code = await _create_account_code(
        db, user, EMAIL_VERIFICATION, enforce_cooldown=True
    )
    background_tasks.add_task(send_verification_email, user.email, code)
    return {"message": "Verification email sent"}


async def resend_verification_by_email(
    db: AsyncSession, email: str, background_tasks: BackgroundTasks
) -> dict:
    user = (
        await db.execute(
            select(User)
            .where(func.lower(User.email) == email.strip().lower())
            .order_by(User.created_at.asc(), User.id.asc())
            .limit(1)
        )
    ).scalar_one_or_none()
    if user and not user.email_verified:
        code = await _create_account_code(
            db, user, EMAIL_VERIFICATION, enforce_cooldown=True
        )
        background_tasks.add_task(send_verification_email, user.email, code)
    return {"message": "If the account exists, a verification code has been sent"}


async def forgot_password(
    db: AsyncSession, data: ForgotPassword, background_tasks: BackgroundTasks
) -> dict:
    identifier = (data.email or data.phone or "").strip()
    if not identifier:
        raise HTTPException(status_code=400, detail="Email or phone required")
    query = (
        select(User)
        .where(func.lower(User.email) == identifier.lower())
        .order_by(User.created_at.asc(), User.id.asc())
        .limit(1)
        if data.email
        else select(User)
        .where(User.phone == identifier)
        .order_by(User.created_at.asc(), User.id.asc())
        .limit(1)
    )
    user = (await db.execute(query)).scalar_one_or_none()
    if user and user.email:
        code = await _create_account_code(
            db, user, PASSWORD_RESET, enforce_cooldown=True
        )
        background_tasks.add_task(send_reset_code_email, user.email, code)
    return {"message": "If the account exists, a reset code has been sent"}


async def reset_password(db: AsyncSession, data: ResetPassword) -> dict:
    user = await _consume_account_code(
        db, email=str(data.email), purpose=PASSWORD_RESET, code=data.code
    )
    user.hashed_password = get_password_hash(data.new_password)
    user.login_attempts = 0
    user.locked_until = None
    now = datetime.now(timezone.utc)
    await db.execute(
        update(RefreshToken)
        .where(RefreshToken.user_id == user.id)
        .values(is_revoked=True, revoked_at=now)
    )
    return {"message": "Password reset successfully"}


async def submit_student_verification(
    db: AsyncSession, user: User, data: StudentVerificationRequest
) -> dict:
    if not user.student_card_image_url:
        raise HTTPException(
            status_code=400,
            detail="Student card evidence must be uploaded before submission",
        )
    existing = await db.execute(
        select(User).where(User.student_id == data.student_id.strip(), User.id != user.id)
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=409, detail="Student ID already linked to another account")
    user.full_name = data.full_name.strip()
    user.student_email = (
        str(data.student_email).strip().lower() if data.student_email else None
    )
    user.university = data.university.strip()
    user.student_id = data.student_id.strip()
    user.academic_year = data.academic_year.strip()
    user.verification_status = "PENDING"
    user.verification_rejection_reason = None
    user.verification_submitted_at = datetime.now(timezone.utc)
    return {"message": "Student verification submitted for review"}


logger = logging.getLogger(__name__)


async def _send_email(recipient: str, subject: str, body: str) -> None:
    if not settings.SMTP_HOST:
        if settings.ENVIRONMENT.lower() == "production":
            raise RuntimeError("SMTP is not configured")
        logger.warning(
            f"SMTP_HOST is not configured. Skipping sending email to {recipient}. "
            f"Subject: {subject}. Body: {body}"
        )
        return
    message = EmailMessage()
    message["From"] = settings.SMTP_FROM_EMAIL
    message["To"] = recipient
    message["Subject"] = subject
    message.set_content(body)
    await aiosmtplib.send(
        message,
        hostname=settings.SMTP_HOST,
        port=settings.SMTP_PORT,
        username=settings.SMTP_USERNAME or None,
        password=settings.SMTP_PASSWORD or None,
        start_tls=settings.SMTP_USE_TLS,
    )


async def send_verification_email(email: str, code: str) -> None:
    await _send_email(
        email,
        "Verify your HiBuddy email",
        f"Your HiBuddy verification code is {code}. It expires in {settings.AUTH_CODE_TTL_MINUTES} minutes.",
    )


async def send_reset_code_email(email: str, code: str) -> None:
    await _send_email(
        email,
        "Reset your HiBuddy password",
        f"Your HiBuddy password reset code is {code}. It expires in {settings.AUTH_CODE_TTL_MINUTES} minutes.",
    )
