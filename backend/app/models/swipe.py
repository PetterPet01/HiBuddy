import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, DateTime, Float, ForeignKey, func, JSON, Index, text
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import UUID
from app.database import Base


class SwipeAction(Base):
    __tablename__ = "swipe_actions"
    __table_args__ = (
        Index(
            "uq_active_swipe_context",
            "swiper_id", "target_type", "target_id", "context_key",
            unique=True,
            postgresql_where=text("is_active = true"),
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    swiper_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    target_type: Mapped[str] = mapped_column(String(20), nullable=False)
    target_id: Mapped[str] = mapped_column(String(36), nullable=False)
    action: Mapped[str] = mapped_column(String(20), nullable=False)
    context_project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=True
    )
    context_role_slot_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("project_role_slots.id", ondelete="SET NULL"), nullable=True
    )
    context_key: Mapped[str] = mapped_column(String(80), default="GLOBAL", nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)


class Match(Base):
    __tablename__ = "matches"
    __table_args__ = (
        Index(
            "uq_active_user_project_match",
            "user_id", "project_id",
            unique=True,
            postgresql_where=text("is_unmatched = false"),
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    project_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=False)
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    role_matched: Mapped[str | None] = mapped_column(String(100), nullable=True)
    match_score: Mapped[float] = mapped_column(Float, default=0.0)
    score_explanation: Mapped[dict | None] = mapped_column(JSON, nullable=True)

    matched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    is_unmatched: Mapped[bool] = mapped_column(Boolean, default=False)
    unmatched_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    chat_hidden_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    is_member_added: Mapped[bool] = mapped_column(Boolean, default=False)


class SwipeQueueItem(Base):
    __tablename__ = "swipe_queue_items"
    __table_args__ = (
        Index(
            "uq_active_queue_target",
            "swiper_id", "target_type", "target_id", "context_key",
            unique=True,
            postgresql_where=text("is_active = true"),
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    swiper_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    target_type: Mapped[str] = mapped_column(String(20), nullable=False)
    target_id: Mapped[str] = mapped_column(String(36), nullable=False)
    context_project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=True
    )
    context_key: Mapped[str] = mapped_column(String(80), default="GLOBAL", nullable=False)
    queued_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    resolved_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    resolution: Mapped[str | None] = mapped_column(String(20), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
