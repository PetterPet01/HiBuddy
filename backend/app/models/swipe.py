import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, DateTime, Float, ForeignKey, func
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import UUID
from app.database import Base


class SwipeAction(Base):
    __tablename__ = "swipe_actions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    swiper_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    target_type: Mapped[str] = mapped_column(String(20), nullable=False)
    target_id: Mapped[str] = mapped_column(String(36), nullable=False)
    action: Mapped[str] = mapped_column(String(20), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)


class Match(Base):
    __tablename__ = "matches"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    project_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=False)
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    role_matched: Mapped[str | None] = mapped_column(String(100), nullable=True)
    match_score: Mapped[float] = mapped_column(Float, default=0.0)

    matched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    is_unmatched: Mapped[bool] = mapped_column(Boolean, default=False)
    unmatched_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    chat_hidden_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    is_member_added: Mapped[bool] = mapped_column(Boolean, default=False)
