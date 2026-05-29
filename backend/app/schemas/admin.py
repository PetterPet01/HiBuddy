from uuid import UUID
from pydantic import BaseModel


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