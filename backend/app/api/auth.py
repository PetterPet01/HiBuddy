from fastapi import APIRouter, Depends, status, BackgroundTasks
from fastapi.security import OAuth2PasswordRequestForm

from app.schemas.auth import (
    UserRegister, UserLogin, UserResponse, TokenResponse,
    TokenRefresh, ForgotPassword, ResetPassword, EmailVerifyRequest,
    StudentVerificationRequest, GoogleLoginRequest, ResendVerificationRequest,
)
from app.services.auth_service import (
    register_user, login_user, refresh_access_token, logout_user,
    verify_email, resend_verification, forgot_password, reset_password,
    submit_student_verification,
    google_login,
    resend_verification_by_email,
)
from app.core.dependencies import get_current_user, get_authenticated_user
from app.database import get_db
from app.models.user import User
from sqlalchemy.ext.asyncio import AsyncSession

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
async def register(
    data: UserRegister,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
):
    return await register_user(db, data, background_tasks)


@router.post("/login-swagger", response_model=TokenResponse)
async def login_swagger(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: AsyncSession = Depends(get_db),
):
    data = UserLogin(username=form_data.username, password=form_data.password, remember_me=False)
    return await login_user(db, data)


@router.post("/login", response_model=TokenResponse)
async def login(
    data: UserLogin,
    db: AsyncSession = Depends(get_db),
):
    return await login_user(db, data)


@router.post("/google", response_model=TokenResponse)
async def login_google(
    data: GoogleLoginRequest,
    db: AsyncSession = Depends(get_db),
):
    return await google_login(db, data)


@router.post("/refresh", response_model=TokenResponse)
async def refresh_token(
    data: TokenRefresh,
    db: AsyncSession = Depends(get_db),
):
    return await refresh_access_token(db, data)


@router.post("/logout")
async def logout(
    data: TokenRefresh | None = None,
    current_user: User = Depends(get_authenticated_user),
    db: AsyncSession = Depends(get_db),
):
    return await logout_user(db, current_user, data.refresh_token if data else None)


@router.post("/verify-email")
async def verify_email_endpoint(
    data: EmailVerifyRequest,
    current_user: User = Depends(get_authenticated_user),
    db: AsyncSession = Depends(get_db),
):
    return await verify_email(db, current_user, data)


@router.post("/resend-verification")
async def resend_verification_endpoint(
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_authenticated_user),
    db: AsyncSession = Depends(get_db),
):
    return await resend_verification(db, current_user, background_tasks)


@router.post("/resend-verification-public")
async def resend_verification_public_endpoint(
    data: ResendVerificationRequest,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
):
    if data.email is None:
        return {"message": "If the account exists, a verification code has been sent"}
    return await resend_verification_by_email(db, str(data.email), background_tasks)


@router.post("/forgot-password")
async def forgot_password_endpoint(
    data: ForgotPassword,
    background_tasks: BackgroundTasks,
    db: AsyncSession = Depends(get_db),
):
    return await forgot_password(db, data, background_tasks)


@router.post("/reset-password")
async def reset_password_endpoint(
    data: ResetPassword,
    db: AsyncSession = Depends(get_db),
):
    return await reset_password(db, data)


@router.post("/verify-student")
async def verify_student(
    data: StudentVerificationRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    return await submit_student_verification(db, current_user, data)
