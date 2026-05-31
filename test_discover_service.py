import sys
from pathlib import Path
sys.path.append(str(Path(__file__).resolve().parent / "backend"))

import asyncio
from sqlalchemy import select
from sqlalchemy.orm import selectinload
from app.database import async_session
from app.models.user import User
from app.services.swipe_service import get_discover_cards

async def main():
    async with async_session() as db:
        user = await db.scalar(
            select(User)
            .options(
                selectinload(User.profile),
                selectinload(User.roles),
                selectinload(User.skills),
                selectinload(User.interests)
            )
            .limit(1)
        )
        cards = await get_discover_cards(db, user, "CONTRIBUTOR", None, 10)
        print("Got project cards:", len(cards.get("project_cards", [])))

asyncio.run(main())
