import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


class RoleCatalog(Base):
    __tablename__ = "role_catalog"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    slug: Mapped[str] = mapped_column(String(80), unique=True, nullable=False)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class SkillCatalog(Base):
    __tablename__ = "skill_catalog"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    slug: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class RoleSkillCatalog(Base):
    __tablename__ = "role_skill_catalog"
    __table_args__ = (
        UniqueConstraint("role_id", "skill_id", name="uq_role_skill_catalog"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    role_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("role_catalog.id", ondelete="CASCADE"), nullable=False
    )
    skill_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("skill_catalog.id", ondelete="CASCADE"), nullable=False
    )


class UserRoleSkill(Base):
    __tablename__ = "user_role_skills"
    __table_args__ = (
        UniqueConstraint("user_role_id", "skill_id", name="uq_user_role_skill"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_role_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("user_roles.id", ondelete="CASCADE"), nullable=False, index=True
    )
    skill_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("skill_catalog.id", ondelete="RESTRICT"), nullable=False
    )
    level: Mapped[str] = mapped_column(String(20), default="BEGINNER", nullable=False)
    needs_improvement: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    skill = relationship("SkillCatalog")


class ProjectRoleSkillRequirement(Base):
    __tablename__ = "project_role_skill_requirements"
    __table_args__ = (
        UniqueConstraint("role_slot_id", "skill_id", name="uq_project_slot_skill"),
    )

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    role_slot_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("project_role_slots.id", ondelete="CASCADE"), nullable=False, index=True
    )
    skill_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("skill_catalog.id", ondelete="RESTRICT"), nullable=False
    )
    minimum_level: Mapped[str] = mapped_column(String(20), default="BEGINNER", nullable=False)
    is_required: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    skill = relationship("SkillCatalog")
