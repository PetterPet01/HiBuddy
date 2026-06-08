import pytest
from uuid import uuid4
from datetime import datetime, timezone
from sqlalchemy import select
from app.database import async_session
from app.models.user import User
from app.models.project import Project, ProjectRoleSlot
from app.models.catalog import ProjectRoleSkillRequirement, SkillCatalog
from app.services.swipe_service import _build_project_queue_card

@pytest.mark.asyncio
async def test_build_project_queue_card_eager_loads_skill_requirements():
    # Use unique usernames/emails to avoid conflicts with previous test runs or seed data
    suffix = str(uuid4())[:8]
    async with async_session() as db:
        # Create a test user (owner)
        from app.models.profile import UserProfile

        owner = User(
            id=uuid4(),
            username=f"owner_user_{suffix}",
            email=f"owner_{suffix}@example.com",
            full_name="Owner User",
            hashed_password="hashed_password",
        )
        db.add(owner)
        await db.flush()
        # Create a profile for owner
        owner_profile = UserProfile(
            id=uuid4(),
            user_id=owner.id,
            display_name="Owner User",
            mode="BOTH",
            reputation_score=5.0,
        )
        db.add(owner_profile)
        await db.flush()
        owner.profile = owner_profile


        # Create a test user (swiper)
        swiper = User(
            id=uuid4(),
            username=f"swiper_user_{suffix}",
            email=f"swiper_{suffix}@example.com",
            full_name="Swiper User",
            hashed_password="hashed_password",
        )
        db.add(swiper)
        await db.flush()
        # Create a profile for swiper
        swiper_profile = UserProfile(
            id=uuid4(),
            user_id=swiper.id,
            display_name="Swiper User",
            mode="BOTH",
            reputation_score=5.0,
        )
        db.add(swiper_profile)
        await db.flush()
        swiper.profile = swiper_profile


        # Create a project
        project = Project(
            id=uuid4(),
            owner_id=owner.id,
            title="Test Project",
            field="EdTech",
            description="A test project description",
            start_date=datetime.now(timezone.utc),
            end_date=datetime.now(timezone.utc),
            max_members=5,
        )
        db.add(project)
        await db.flush()

        # Create a role slot
        slot = ProjectRoleSlot(
            id=uuid4(),
            project_id=project.id,
            role_name="Backend Developer",
            count=2,
            filled=0,
        )
        db.add(slot)
        await db.flush()

        # Create a skill catalog entry
        # Check if python skill already exists in catalog
        res = await db.execute(select(SkillCatalog).where(SkillCatalog.slug == "python"))
        skill = res.scalar_one_or_none()
        if not skill:
            skill = SkillCatalog(
                id=uuid4(),
                slug="python",
                name="Python",
            )
            db.add(skill)
            await db.flush()
        # Already handled above

        # Create a skill requirement
        req = ProjectRoleSkillRequirement(
            id=uuid4(),
            role_slot_id=slot.id,
            skill_id=skill.id,
            minimum_level="INTERMEDIATE",
            is_required=True,
        )
        db.add(req)
        await db.flush()
        await db.commit()

        # Fetch swiper with eager loaded options like get_authenticated_user does
        from app.models.profile import UserRole
        from app.models.catalog import UserRoleSkill
        from sqlalchemy.orm import selectinload
        res = await db.execute(
            select(User)
            .options(
                selectinload(User.profile),
                selectinload(User.roles).selectinload(UserRole.role_skills).selectinload(UserRoleSkill.skill),
                selectinload(User.skills),
                selectinload(User.interests),
            )
            .where(User.id == swiper.id)
        )
        swiper_loaded = res.scalar_one()

        # Call _build_project_queue_card and verify it doesn't raise MissingGreenlet
        card = await _build_project_queue_card(db, swiper_loaded, project.id)
        assert card is not None
        assert card["title"] == "Test Project"
        assert len(card["role_slots"]) == 1
        assert card["role_slots"][0]["role_name"] == "Backend Developer"

        # Clean up
        # Since we committed, we should delete in reverse order of dependencies to avoid foreign key constraint issues
        await db.delete(req)
        await db.delete(slot)
        await db.delete(project)
        await db.delete(swiper_profile)
        await db.delete(swiper)
        await db.delete(owner_profile)
        await db.delete(owner)
        await db.commit()
