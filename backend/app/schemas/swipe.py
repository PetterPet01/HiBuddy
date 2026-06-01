from datetime import datetime
from uuid import UUID
from pydantic import BaseModel


class SwipeActionRequest(BaseModel):
    target_type: str
    target_id: str
    action: str


class QueueAddRequest(BaseModel):
    target_type: str
    target_id: str


class QueueDecisionRequest(BaseModel):
    action: str


class DiscoverResponse(BaseModel):
    user_cards: list = []
    project_cards: list = []
    next_cursor: str | None = None
    daily_likes_remaining: int = 50
    daily_superlikes_remaining: int = 3


class MatchResponse(BaseModel):
    id: UUID
    user_id: UUID
    project_id: UUID
    owner_id: UUID
    role_matched: str | None
    match_score: float
    matched_at: datetime
    is_unmatched: bool
    is_member_added: bool
    user_name: str | None = None
    user_avatar: str | None = None
    user_is_online: bool = False
    user_last_seen_at: datetime | None = None
    project_title: str | None = None
    last_message: str | None = None
    last_message_time: datetime | None = None
    is_unread: bool = False

    class Config:
        from_attributes = True


class ApplicantResponse(BaseModel):
    user_id: UUID
    display_name: str
    avatar_url: str | None
    roles: list
    skills: list
    verified_student: bool
    reputation_score: float
    match_score: float
    swiped_at: datetime


class QueueItemResponse(BaseModel):
    id: UUID
    target_type: str
    target_id: str
    queued_at: datetime
    expires_at: datetime
    seconds_remaining: int
    user_card: dict | None = None
    project_card: dict | None = None


class QueueResponse(BaseModel):
    user_profiles: list[QueueItemResponse] = []
    project_profiles: list[QueueItemResponse] = []
    user_capacity_remaining: int = 3
    project_capacity_remaining: int = 3
