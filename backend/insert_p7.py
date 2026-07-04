import asyncio
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from app.config import get_settings
from seed_data import (
    P7, U1, days_from_now, days_ago, SLOT11, MEM12
)
from app.models.project import Project, ProjectRoleSlot, ProjectMember

settings = get_settings()

async def insert_p7():
    engine = create_async_engine(settings.DATABASE_URL, echo=False)
    async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    
    async with async_session() as db:
        p7 = Project(
            id=P7, owner_id=U1,
            title="Crypto Scam - Free Money", field="Finance",
            description="Join now to make $1000 a day without any effort. Click this suspicious link! We guarantee 100% returns on your investment in just a week.",
            specific_goal="Scam innocent users",
            work_mode="ONLINE", commitment_level="CASUAL",
            start_date=days_from_now(1), end_date=days_from_now(30),
            max_members=5, status="RECRUITING", review_status="FLAGGED",
            moderation_categories=["spam", "financial_scam"],
            moderation_reasons=["Automated moderation flagged this for scam and spam keywords"],
            moderation_checked_at=days_ago(0),
            additional_requirements="None",
            member_benefits="Free money",
            created_at=days_ago(0),
        )
        db.add(p7)
        
        slot11 = ProjectRoleSlot(
            id=SLOT11, project_id=P7, role_name="Victim", count=4, filled=0,
            skill_requirements=None
        )
        db.add(slot11)
        
        mem12 = ProjectMember(
            id=MEM12, project_id=P7, user_id=U1, role="Project Owner", is_owner=True, joined_at=days_ago(0)
        )
        db.add(mem12)
        
        await db.commit()
        print("Successfully inserted P7 and its relations")

if __name__ == "__main__":
    asyncio.run(insert_p7())
