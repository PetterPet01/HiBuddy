import asyncio
from sqlalchemy import select
from app.database import async_session
from app.models.project import Project

async def main():
    async with async_session() as db:
        query = select(Project).where(Project.status == "RECRUITING")
        res = await db.execute(query)
        projects = res.scalars().all()
        print(f"Total projects with RECRUITING status: {len(projects)}")
        for p in projects:
            print(p.title)

asyncio.run(main())
