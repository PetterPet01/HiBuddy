import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.pool import NullPool
from typing import AsyncGenerator
from app.database import Base
from app.config import get_settings

settings = get_settings()

@pytest.fixture(scope="session")
def anyio_backend():
    return "asyncio"

# Create a brand new engine for testing to avoid shared state across tests
# This engine is used *only* for tests and correctly scoped
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    poolclass=NullPool,
)

TestSession = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)

@pytest_asyncio.fixture(scope="function")
async def db() -> AsyncGenerator[AsyncSession, None]:
    """Provide a database session that rolls back after each test."""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        
    async with TestSession() as session:
        yield session
        await session.rollback()
