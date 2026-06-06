from pydantic import BaseModel, ConfigDict
from uuid import UUID
from datetime import datetime
from typing import Optional

class UserBlockCreate(BaseModel):
    blocked_id: UUID
    reason: Optional[str] = None

class UserBlockResponse(BaseModel):
    id: UUID
    blocker_id: UUID
    blocked_id: UUID
    reason: Optional[str]
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class ReportCreate(BaseModel):
    reported_id: UUID
    reason: str
    description: Optional[str] = None
    evidence_url: Optional[str] = None
    context_type: Optional[str] = None
    context_id: Optional[str] = None

class ReportResponse(BaseModel):
    id: UUID
    reporter_id: UUID
    reported_id: UUID
    reason: str
    description: Optional[str]
    status: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
