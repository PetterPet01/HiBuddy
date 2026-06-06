from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field
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
    verification_status: str
    verification_rejection_reason: str | None = None
    academic_year: str | None = None
    student_card_image_url: str | None = None
    verification_submitted_at: datetime | None = None
    role: str
    is_active: bool

    model_config = ConfigDict(from_attributes=True)


class RejectStudentRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=500)


class AdminActionRequest(BaseModel):
    reason: str = Field(min_length=3, max_length=500)

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
