from pydantic import BaseModel, ConfigDict
from uuid import UUID
from datetime import datetime
from typing import Optional

class FCMTokenRegister(BaseModel):
    token: str
    device_type: Optional[str] = "android"

class FCMTokenResponse(BaseModel):
    id: UUID
    user_id: UUID
    token: str
    device_type: Optional[str]
    is_active: bool
    last_used_at: datetime
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
