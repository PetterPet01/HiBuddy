from datetime import datetime, date
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator
from typing import Literal


class SkillRequirementCreate(BaseModel):
    skill_name: str = Field(min_length=1, max_length=100)
    minimum_level: Literal["BEGINNER", "INTERMEDIATE", "ADVANCED"] = "BEGINNER"
    is_required: bool = True


class RoleSlotCreate(BaseModel):
    role_name: str = Field(min_length=1, max_length=100)
    count: int = Field(ge=1, le=20)
    skill_requirements: list[SkillRequirementCreate] = Field(default_factory=list, max_length=30)

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

    model_config = ConfigDict(from_attributes=True)


class ProjectCreate(BaseModel):
    title: str = Field(min_length=3, max_length=200)
    field: str = Field(min_length=2, max_length=50)
    description: str = Field(min_length=20, max_length=500)
    specific_goal: str | None = None
    start_date: str
    end_date: str
    max_members: int = Field(ge=2, le=50)
    work_mode: Literal["ONLINE", "OFFLINE", "HYBRID"] = "ONLINE"
    commitment_level: Literal["CASUAL", "MODERATE", "INTENSIVE"] = "CASUAL"
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

    @model_validator(mode="after")
    def validate_capacity(self):
        if sum(slot.count for slot in self.role_slots) + 1 > self.max_members:
            raise ValueError("Max members must include the owner and all recruiting slots")
        return self


class ProjectUpdate(BaseModel):
    title: str | None = None
    description: str | None = None
    specific_goal: str | None = None
    start_date: str | None = None
    end_date: str | None = None
    work_mode: Literal["ONLINE", "OFFLINE", "HYBRID"] | None = None
    commitment_level: Literal["CASUAL", "MODERATE", "INTENSIVE"] | None = None
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
    review_status: str = "APPROVED"
    moderation_categories: list[str] | None = None
    moderation_reasons: list[str] | None = None
    additional_requirements: str | None
    member_benefits: str | None
    role_slots: list[RoleSlotResponse] = []
    members: list[ProjectMemberResponse] = []
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


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

    model_config = ConfigDict(from_attributes=True)
