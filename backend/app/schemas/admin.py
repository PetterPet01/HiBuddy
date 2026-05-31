from uuid import UUID
from pydantic import BaseModel
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
    role: str
    is_active: bool

    class Config:
        from_attributes = True


class RejectStudentRequest(BaseModel):
    reason: str

class AdminReportResponse(BaseModel):
    id: UUID
    reporter_id: UUID
    reported_id: UUID
    reason: str
    description: str | None = None
    status: str
    created_at: datetime

    reporter_name: str | None = None
    reported_name: str | None = None

    class Config:
        from_attributes = True


class ResolveReportRequest(BaseModel):
    action: str  # "DISMISS" hoặc "BAN"