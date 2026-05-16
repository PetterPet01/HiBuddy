from datetime import datetime, date
from uuid import UUID
from pydantic import BaseModel, field_validator


class RoleSlotCreate(BaseModel):
    role_name: str
    count: int
    skill_requirements: str | None = None

    @field_validator("count")
    @classmethod
    def count_positive(cls, v: int) -> int:
        if v < 1:
            raise ValueError("Count must be at least 1")
        return v


class RoleSlotResponse(BaseModel):
    id: UUID
    role_name: str
    count: int
    filled: int
    skill_requirements: dict | None = None

    class Config:
        from_attributes = True


class ProjectCreate(BaseModel):
    title: str
    field: str
    description: str
    specific_goal: str | None = None
    start_date: str
    end_date: str
    max_members: int
    work_mode: str = "ONLINE"
    commitment_level: str = "CASUAL"
    role_slots: list[RoleSlotCreate]
    additional_requirements: str | None = None
    member_benefits: str | None = None

    @field_validator("max_members")
    @classmethod
    def validate_max_members(cls, v: int) -> int:
        if v < 1:
            raise ValueError("Max members must be at least 1")
        return v

    @field_validator("role_slots")
    @classmethod
    def validate_role_slots(cls, v: list[RoleSlotCreate]) -> list[RoleSlotCreate]:
        if not v:
            raise ValueError("At least one role slot is required")
        return v


class ProjectUpdate(BaseModel):
    title: str | None = None
    description: str | None = None
    specific_goal: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    work_mode: str | None = None
    commitment_level: str | None = None
    additional_requirements: str | None = None
    member_benefits: str | None = None


class ProjectMemberResponse(BaseModel):
    id: UUID
    user_id: UUID
    display_name: str
    role: str
    is_owner: bool
    avatar_url: str | None
    joined_at: datetime


class ProjectResponse(BaseModel):
    id: UUID
    owner_id: UUID
    title: str
    field: str
    description: str
    specific_goal: str | None
    thumbnail_url: str | None
    work_mode: str
    commitment_level: str
    start_date: datetime
    end_date: datetime
    max_members: int
    status: str
    additional_requirements: str | None
    member_benefits: str | None
    role_slots: list[RoleSlotResponse] = []
    members: list[ProjectMemberResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True


class ProjectCardResponse(BaseModel):
    project_id: UUID
    title: str
    field: str
    description: str
    owner_name: str
    owner_avatar: str | None
    owner_verified: bool
    role_slots: list[RoleSlotResponse]
    work_mode: str
    commitment_level: str
    start_date: datetime
    end_date: datetime
    total_slots: int
    filled_slots: int
    match_score: float = 0.0

    class Config:
        from_attributes = True
