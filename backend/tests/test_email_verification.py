import pytest
from uuid import uuid4
from fastapi import BackgroundTasks, HTTPException
from sqlalchemy import delete, select

from app.core.security import get_password_hash
from app.database import async_session
from app.models.chat import RefreshToken
from app.models.user import User
from app.models.auth import AccountToken
from app.schemas.auth import EmailVerifyRequest, UserLogin, UserRegister
from app.services.auth_service import (
    _create_account_code,
    EMAIL_VERIFICATION,
    login_user,
    register_user,
    resend_verification_by_email,
    verify_email,
)


@pytest.mark.asyncio
async def test_email_verification_flow():
    suffix = str(uuid4())[:8]
    async with async_session() as db:
        # 1. Create a test user
        user = User(
            id=uuid4(),
            username=f"verify_user_{suffix}",
            email=f"verify_{suffix}@example.com",
            full_name="Verify User",
            hashed_password=get_password_hash("SecurePass1"),
            email_verified=False,
        )
        db.add(user)
        await db.flush()

        # 2. Create verification code
        code = await _create_account_code(db, user, EMAIL_VERIFICATION, enforce_cooldown=False)
        assert len(code) == 6
        assert code.isdigit()

        # 3. Verify that the code is stored in the database
        res = await db.execute(
            select(AccountToken)
            .where(AccountToken.user_id == user.id, AccountToken.purpose == EMAIL_VERIFICATION)
        )
        token = res.scalar_one_or_none()
        assert token is not None
        assert token.consumed_at is None

        # 4. Valid credentials only create a limited verification session.
        login_response = await login_user(
            db,
            UserLogin(
                username=user.email,
                password="SecurePass1",
                remember_me=False,
            ),
        )
        assert login_response.requires_email_verification is True
        assert login_response.user.email_verified is False

        # 5. The verification request cannot be redirected to another email.
        with pytest.raises(HTTPException) as exc_info:
            await verify_email(
                db,
                user,
                EmailVerifyRequest(email=f"other_{suffix}@example.com", code=code),
            )
        assert exc_info.value.status_code == 403
        assert user.email_verified is False
        assert token.consumed_at is None

        # 6. Verify the signed-in account with its code.
        result = await verify_email(
            db,
            user,
            EmailVerifyRequest(email=user.email, code=code),
        )
        assert result["message"] == "Email verified successfully"
        assert user.email_verified is True
        assert token.consumed_at is not None

        # 7. Clean up
        await db.execute(delete(RefreshToken).where(RefreshToken.user_id == user.id))
        await db.delete(token)
        await db.delete(user)
        await db.commit()



@pytest.mark.asyncio
async def test_register_duplicate_email_or_username_returns_conflict():
    class DuplicateRowsResult:
        def scalar_one_or_none(self):
            raise AssertionError("register_user should not require one-or-none cardinality")

        def scalar(self):
            return object()

    class StubSession:
        async def execute(self, statement):
            return DuplicateRowsResult()

    with pytest.raises(HTTPException) as exc_info:
        await register_user(
            StubSession(),
            UserRegister(
                full_name="New Duplicate User",
                username="duplicate_user",
                email="duplicate@example.com",
                date_of_birth="01/01/2000",
                password="SecurePass1",
                confirm_password="SecurePass1",
                agree_terms=True,
            ),
            BackgroundTasks(),
        )

    assert exc_info.value.status_code == 409
    assert exc_info.value.detail == "Email or username already registered"


@pytest.mark.asyncio
async def test_public_resend_verification_sends_code_for_logged_out_unverified_user(monkeypatch):
    sent_codes: list[tuple[str, str]] = []
    user = User(
        id=uuid4(),
        username="public_resend_user",
        email="public_resend@example.com",
        full_name="Public Resend User",
        hashed_password=get_password_hash("SecurePass1"),
        email_verified=False,
    )

    class QueryResult:
        def scalar_one_or_none(self):
            return user

    class CodeResult:
        def scalar_one_or_none(self):
            return None

    class StubSession:
        def __init__(self):
            self.execute_count = 0
            self.added = []

        async def execute(self, statement):
            self.execute_count += 1
            return QueryResult() if self.execute_count == 1 else CodeResult()

        def add(self, instance):
            self.added.append(instance)

    async def capture_email(email: str, code: str) -> None:
        sent_codes.append((email, code))

    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email",
        capture_email,
    )

    db = StubSession()
    background_tasks = BackgroundTasks()
    result = await resend_verification_by_email(db, user.email, background_tasks)
    await background_tasks()

    assert result["message"] == "If the account exists, a verification code has been sent"
    assert len(db.added) == 1
    assert isinstance(db.added[0], AccountToken)
    assert db.added[0].user_id == user.id
    assert db.added[0].purpose == EMAIL_VERIFICATION
    assert sent_codes == [(user.email, sent_codes[0][1])]
    assert len(sent_codes[0][1]) == 6
    assert sent_codes[0][1].isdigit()