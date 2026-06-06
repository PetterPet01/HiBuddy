from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, ConfigDict, Field, field_validator
from typing import Literal


class TaskCreate(BaseModel):
    title: str = Field(min_length=2, max_length=300)
    description: str | None = Field(default=None, max_length=2000)
    assignee_id: str
    role_related: str | None = None
    priority: Literal["LOW", "MEDIUM", "HIGH", "URGENT"] = "MEDIUM"
    start_date: str
    deadline: str
    tag: str | None = None


class TaskUpdate(BaseModel):
    title: str | None = None
    description: str | None = None
    priority: Literal["LOW", "MEDIUM", "HIGH", "URGENT"] | None = None
    deadline: str | None = None
    tag: str | None = None
    assignee_id: str | None = None


class TaskStatusUpdate(BaseModel):
    status: Literal["TODO", "IN_PROGRESS", "DONE_REVIEW", "CLOSED"]


class TaskCheckoutOverride(BaseModel):
    checkout_status: Literal["EARLY", "ON_TIME", "LATE", "LATE_CHECKOUT", "NOT_COMPLETED"]
    notes: str | None = None


class TaskResponse(BaseModel):
    id: UUID
    project_id: UUID
    assignee_id: UUID
    creator_id: UUID
    title: str
    description: str | None
    role_related: str | None
    priority: str
    status: str
    start_date: datetime
    deadline: datetime
    tag: str | None
    checkout_at: datetime | None
    checkout_confirmed_at: datetime | None
    checkout_status: str | None
    assignee_name: str | None = None
    assignee_avatar: str | None = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class DashboardResponse(BaseModel):
    project_id: UUID
    project_title: str
    total_tasks: int
    total_members: int
    member_stats: list


class MemberStatResponse(BaseModel):
    user_id: UUID
    display_name: str
    role: str
    total_tasks: int
    early: int
    on_time: int
    late: int
    in_progress: int
    todo: int


class EvaluationCreate(BaseModel):
    quality_score: float
    collaboration_score: float
    communication_score: float
    deadline_score: float
    feedback_text: str | None = None

    @field_validator("quality_score", "collaboration_score", "communication_score", "deadline_score")
    @classmethod
    def validate_score(cls, v: float) -> float:
        if v < 0 or v > 5:
            raise ValueError("Score must be between 0 and 5")
        return round(v, 1)


class EvaluationResponse(BaseModel):
    id: UUID
    project_id: UUID
    evaluator_id: UUID
    evaluatee_id: UUID
    quality_score: float
    collaboration_score: float
    communication_score: float
    deadline_score: float
    overall_score: float
    feedback_text: str | None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
