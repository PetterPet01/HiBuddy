import asyncio
from sqlalchemy import select
from app.database import async_session
from app.models.project import Project

async def main():
    async with async_session() as db:
        query = select(Project).where(Project.status == "RECRUITING", ~Project.id.in_([]))
        res = await db.execute(query)
        print("Empty list exclusion:", len(res.scalars().all()))

        query = select(Project).where(Project.status == "RECRUITING")
        res = await db.execute(query)
        print("No exclusion:", len(res.scalars().all()))

asyncio.run(main())
