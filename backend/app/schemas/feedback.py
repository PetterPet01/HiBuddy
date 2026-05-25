from datetime import datetime
from uuid import UUID
from pydantic import BaseModel, field_validator


class FeedbackCreate(BaseModel):
    feedback_text: str

    @field_validator("feedback_text")
    @classmethod
    def not_empty(cls, v: str) -> str:
        v = v.strip()
        if not v or len(v) < 10:
            raise ValueError("Feedback must be at least 10 characters")
        if len(v) > 1000:
            raise ValueError("Feedback must be at most 1000 characters")
        return v


class FeedbackResponse(BaseModel):
    id: UUID
    project_id: UUID
    target_id: UUID
    feedback_text: str
    analyzed_weaknesses: list[str] | None = None
    created_at: datetime

    class Config:
        from_attributes = True


class MyFeedbackSummary(BaseModel):
    total_feedbacks: int
    weaknesses: list[str]
    feedbacks: list[FeedbackResponse]
