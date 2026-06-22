from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field, field_validator
from datetime import datetime

class AdminUserResponse(BaseModel):
    id: UUID
    username: str
    email: str
    full_name: str
    verified_student: bool
    student_email: str | None = None
    university: str | None = None
    student_id: str | None = None
    student_email_domain: str | None = None
    verification_status: str
    verification_rejection_reason: str | None = None
    academic_year: str | None = None
    student_card_image_url: str | None = None
    verification_document_type: str | None = None
    verification_submitted_at: datetime | None = None
    verification_reviewed_at: datetime | None = None
    verification_reviewed_by: UUID | None = None
    role: str
    is_active: bool

    model_config = ConfigDict(from_attributes=True)


class RejectStudentRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=500)


class AdminActionRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=500)


class AdminRoleUpdateRequest(BaseModel):
    role: str
    reason: str = Field(min_length=3, max_length=500)

    @field_validator("role")
    @classmethod
    def validate_role(cls, value: str) -> str:
        normalized = value.strip().upper()
        if normalized not in {"MEMBER", "MODERATOR"}:
            raise ValueError("Role must be MEMBER or MODERATOR")
        return normalized


class StaffOverviewResponse(BaseModel):
    total_users: int
    active_users: int
    banned_users: int
    verified_students: int
    pending_verifications: int
    open_reports: int
    flagged_projects: int
    admin_users: int
    moderator_users: int

class AdminReportResponse(BaseModel):
    id: UUID
    reporter_id: UUID
    reported_id: UUID
    reason: str
    description: str | None = None
    evidence_url: str | None = None
    context_type: str | None = None
    context_id: str | None = None
    status: str
    created_at: datetime

    reporter_name: str | None = None
    reported_name: str | None = None

    model_config = ConfigDict(from_attributes=True)


class ResolveReportRequest(BaseModel):
    action: str
    reason: str = Field(min_length=3, max_length=500)
