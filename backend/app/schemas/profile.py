from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, field_validator
import re


class SkillCreate(BaseModel):
    skill_name: str
    level: str = "BEGINNER"
    needs_improvement: bool = False


class SkillResponse(BaseModel):
    id: UUID
    skill_name: str
    level: str
    needs_improvement: bool

    class Config:
        from_attributes = True


class RoleCreate(BaseModel):
    role_name: str
    ordering: int = 0


class RoleResponse(BaseModel):
    id: UUID
    role_name: str
    ordering: int

    class Config:
        from_attributes = True


class InterestCreate(BaseModel):
    interest_name: str


class InterestResponse(BaseModel):
    id: UUID
    interest_name: str

    class Config:
        from_attributes = True


class ProfileCreate(BaseModel):
    display_name: str
    bio: str | None = None
    location: str | None = None
    portfolio_url: str | None = None
    github_url: str | None = None
    facebook_url: str | None = None
    short_term_goal: str | None = None
    mode: str = "BOTH"

    @field_validator("portfolio_url")
    @classmethod
    def validate_url(cls, v: str | None) -> str | None:
        if v and not v.startswith("https://"):
            raise ValueError("Portfolio URL must start with https://")
        return v


class ProfileUpdate(ProfileCreate):
    pass


class ProfileResponse(BaseModel):
    id: UUID
    user_id: UUID
    display_name: str
    bio: str | None
    location: str | None
    portfolio_url: str | None
    github_url: str | None
    facebook_url: str | None
    short_term_goal: str | None
    mode: str
    is_hidden: bool
    reputation_score: float
    projects_completed: int
    avatar_url: str | None
    email: str
    verified_student: bool
    university: str | None
    roles: list[RoleResponse] = []
    skills: list[SkillResponse] = []
    interests: list[InterestResponse] = []
    created_at: datetime

    class Config:
        from_attributes = True


class UserCardResponse(BaseModel):
    user_id: UUID
    display_name: str
    avatar_url: str | None
    verified_student: bool
    university: str | None
    bio: str | None
    roles: list[RoleResponse]
    skills: list[SkillResponse]
    location: str | None
    github_url: str | None
    reputation_score: float
    projects_completed: int
    match_score: float = 0.0

    class Config:
        from_attributes = True


class CompletedCourseCreate(BaseModel):
    course_title: str
    source: str
    course_id: str | None = None


class CompletedCourseResponse(BaseModel):
    id: UUID
    course_title: str
    source: str
    badge_visible: bool
    completed_date: datetime

    class Config:
        from_attributes = True
