from uuid import uuid4

import pytest
from fastapi import HTTPException

from app.api.endpoints.admin import (
    approve_student_verification,
    reject_student_verification,
)
from app.models.chat import Notification
from app.models.operations import AdminAuditLog
from app.models.user import User
from app.schemas.admin import RejectStudentRequest
from app.schemas.auth import StudentVerificationRequest
from app.services.auth_service import submit_student_verification


class _ScalarResult:
    def __init__(self, value):
        self._value = value

    def scalar_one_or_none(self):
        return self._value


class _ScalarsResult:
    def __init__(self, values):
        self._values = values

    def scalars(self):
        return self

    def all(self):
        return self._values


class StubSession:
    def __init__(self, execute_results):
        self._execute_results = list(execute_results)
        self.added = []

    async def execute(self, statement):
        if not self._execute_results:
            raise AssertionError("Unexpected database execute call")
        return self._execute_results.pop(0)

    def add(self, instance):
        self.added.append(instance)


@pytest.mark.asyncio
async def test_submit_student_verification_sets_pending_and_notifies_admins():
    user = User(
        id=uuid4(),
        username="student_user",
        email="student@example.com",
        full_name="Student User",
        student_card_image_url="https://example.com/card.jpg",
        verified_student=True,
        verification_status="REJECTED",
    )
    admins = [
        User(id=uuid4(), username="admin1", email="admin1@example.com", full_name="Admin 1", role="ADMIN"),
        User(id=uuid4(), username="admin2", email="admin2@example.com", full_name="Admin 2", role="ADMIN"),
    ]
    db = StubSession([
        _ScalarResult(None),
        _ScalarsResult(admins),
    ])

    result = await submit_student_verification(
        db,
        user,
        StudentVerificationRequest(
            full_name="Student User",
            student_email="student@school.edu",
            university="Example University",
            student_id="SV12345",
            academic_year="Year 3",
        ),
    )

    assert result["message"] == "Student verification submitted for review"
    assert user.verified_student is False
    assert user.verification_status == "PENDING"
    assert user.verification_rejection_reason is None
    notifications = [item for item in db.added if isinstance(item, Notification)]
    assert len(notifications) == 2
    assert all(item.type == "STUDENT_VERIFICATION_SUBMITTED" for item in notifications)


@pytest.mark.asyncio
async def test_approve_student_verification_rejects_non_pending_requests():
    admin = User(id=uuid4(), username="admin", email="admin@example.com", full_name="Admin", role="ADMIN")
    user = User(
        id=uuid4(),
        username="member",
        email="member@example.com",
        full_name="Member User",
        student_card_image_url="https://example.com/card.jpg",
        verification_status="APPROVED",
    )
    db = StubSession([_ScalarResult(user)])

    with pytest.raises(HTTPException) as exc_info:
        await approve_student_verification(user.id, current_user=admin, db=db)

    assert exc_info.value.status_code == 409
    assert exc_info.value.detail == "Student verification request is not pending"


@pytest.mark.asyncio
async def test_reject_student_verification_sets_reason_and_notifies_user():
    admin = User(id=uuid4(), username="admin", email="admin@example.com", full_name="Admin", role="ADMIN")
    user = User(
        id=uuid4(),
        username="member",
        email="member@example.com",
        full_name="Member User",
        student_card_image_url="https://example.com/card.jpg",
        verification_status="PENDING",
    )
    db = StubSession([_ScalarResult(user)])

    response = await reject_student_verification(
        user.id,
        RejectStudentRequest(reason="Please upload a clearer card image."),
        current_user=admin,
        db=db,
    )

    assert response is user
    assert user.verified_student is False
    assert user.verification_status == "REJECTED"
    assert user.verification_rejection_reason == "Please upload a clearer card image."
    assert any(isinstance(item, AdminAuditLog) and item.action == "REJECT_STUDENT" for item in db.added)
    assert any(
        isinstance(item, Notification)
        and item.type == "STUDENT_VERIFICATION_REJECTED"
        and item.body == "Please upload a clearer card image."
        for item in db.added
    )
