import uuid
from datetime import datetime, timezone
from sqlalchemy import String, Boolean, DateTime, Integer, ForeignKey, func, JSON, UniqueConstraint, CheckConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.dialects.postgresql import UUID
from app.database import Base


class Project(Base):
    __tablename__ = "projects"
    __table_args__ = (
        CheckConstraint("end_date > start_date", name="ck_project_date_range"),
        CheckConstraint("max_members >= 2 AND max_members <= 50", name="ck_project_member_limit"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    title: Mapped[str] = mapped_column(String(200), nullable=False)
    field: Mapped[str] = mapped_column(String(50), nullable=False)
    description: Mapped[str] = mapped_column(String(500), nullable=False)
    specific_goal: Mapped[str] = mapped_column(String(500), nullable=True)
    thumbnail_url: Mapped[str | None] = mapped_column(String(500), nullable=True)

    work_mode: Mapped[str] = mapped_column(String(20), default="ONLINE")
    commitment_level: Mapped[str] = mapped_column(String(20), default="CASUAL")

    start_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    max_members: Mapped[int] = mapped_column(Integer, nullable=False)

    status: Mapped[str] = mapped_column(String(20), default="RECRUITING")
    review_status: Mapped[str] = mapped_column(String(20), default="APPROVED")
    moderation_categories: Mapped[list | None] = mapped_column(JSON, nullable=True)
    moderation_reasons: Mapped[list | None] = mapped_column(JSON, nullable=True)
    moderation_checked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    additional_requirements: Mapped[str | None] = mapped_column(String(500), nullable=True)
    member_benefits: Mapped[str | None] = mapped_column(String(500), nullable=True)

    embedding_id: Mapped[int | None] = mapped_column(Integer, nullable=True)
    embedding_updated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    role_slots = relationship("ProjectRoleSlot", back_populates="project", cascade="all, delete-orphan")
    members = relationship("ProjectMember", back_populates="project", cascade="all, delete-orphan")


class ProjectRoleSlot(Base):
    __tablename__ = "project_role_slots"
    __table_args__ = (
        CheckConstraint("count > 0", name="ck_role_slot_count"),
        CheckConstraint("filled >= 0 AND filled <= count", name="ck_role_slot_filled"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    project_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=False)

    role_name: Mapped[str] = mapped_column(String(100), nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False)
    filled: Mapped[int] = mapped_column(Integer, default=0)
    skill_requirements: Mapped[dict | None] = mapped_column(JSON, nullable=True)

    project = relationship("Project", back_populates="role_slots")
    skill_requirements_rows = relationship(
        "ProjectRoleSkillRequirement", cascade="all, delete-orphan"
    )


class ProjectMember(Base):
    __tablename__ = "project_members"
    __table_args__ = (
        UniqueConstraint("project_id", "user_id", name="uq_project_member"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    project_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("projects.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    role: Mapped[str] = mapped_column(String(100), nullable=False)
    is_owner: Mapped[bool] = mapped_column(Boolean, default=False)
    joined_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    project = relationship("Project", back_populates="members")
