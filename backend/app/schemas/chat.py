from datetime import datetime
from uuid import UUID
from pydantic import BaseModel


class CourseSuggestionResponse(BaseModel):
    id: UUID
    target_skill: str
    course_title: str
    course_id: str
    source: str
    url: str | None
    match_percent: float
    is_dismissed: bool

    class Config:
        from_attributes = True


class MentorSuggestionResponse(BaseModel):
    user_id: UUID
    display_name: str
    avatar_url: str | None
    verified_student: bool
    university: str | None
    bio: str | None
    roles: list
    skills: list
    reputation_score: float
    match_score: float


class MessageResponse(BaseModel):
    id: UUID
    chat_id: UUID
    sender_id: UUID
    content: str
    is_read: bool
    created_at: datetime
    sender_name: str | None = None

    class Config:
        from_attributes = True


class ProjectInvitationCreate(BaseModel):
    role_slot_id: UUID
    message: str | None = None


class InvitationRoleSlotResponse(BaseModel):
    id: UUID
    role_name: str
    count: int
    filled: int


class ProjectInvitationOptionsResponse(BaseModel):
    can_invite: bool
    reason: str | None = None
    project_id: UUID | None = None
    project_title: str | None = None
    open_role_slots: list[InvitationRoleSlotResponse] = []


class ProjectInvitationResponse(BaseModel):
    id: UUID
    match_id: UUID
    project_id: UUID
    project_title: str
    inviter_id: UUID
    inviter_name: str
    invitee_id: UUID
    invitee_name: str
    role_slot_id: UUID | None
    role: str
    message: str | None
    status: str
    created_at: datetime
    responded_at: datetime | None = None
    is_incoming: bool = False
    is_outgoing: bool = False


class ChatResponse(BaseModel):
    id: UUID
    match_id: UUID
    user_name: str
    user_avatar: str | None
    project_title: str
    last_message: str | None
    last_message_time: datetime | None
    is_unread: bool = False

    class Config:
        from_attributes = True


class NotificationResponse(BaseModel):
    id: UUID
    type: str
    title: str
    body: str
    is_read: bool
    related_id: str | None
    created_at: datetime

    class Config:
        from_attributes = True
