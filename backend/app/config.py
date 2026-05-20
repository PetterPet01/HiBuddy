from pydantic_settings import BaseSettings
from functools import lru_cache
from pathlib import Path


BACKEND_DIR = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    APP_NAME: str = "HiBuddy"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    SECRET_KEY: str = "change-me-in-production-use-a-strong-random-secret-key"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    DATABASE_URL: str = "postgresql+asyncpg://hibuddy:hibuddy_secret@localhost:5432/hibuddy"
    DATABASE_URL_SYNC: str = "postgresql://hibuddy:hibuddy_secret@localhost:5432/hibuddy"

    REDIS_URL: str = "redis://localhost:6379/0"

    MILVUS_HOST: str = "localhost"
    MILVUS_PORT: int = 19530

    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "hibuddy"
    MINIO_SECURE: bool = False

    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""

    CORS_ORIGINS: list[str] = ["*"]

    SENTENCE_TRANSFORMER_MODEL: str = "all-MiniLM-L6-v2"
    EMBEDDING_DIM: int = 384
    ENABLE_EMBEDDINGS: bool = False
    ENABLE_MILVUS: bool = False
    MILVUS_CONNECT_TIMEOUT_SECONDS: float = 0.5

    SWIPE_DAILY_LIKE_LIMIT: int = 50
    SWIPE_DAILY_SUPERLIKE_LIMIT: int = 3
    PASS_COOLDOWN_DAYS: int = 7
    MAX_ACTIVE_PROJECTS_PER_OWNER: int = 3
    CHECKOUT_REVIEW_HOURS: int = 72
    GRACE_PERIOD_HOURS: int = 12
    MAX_COURSE_SUGGESTIONS: int = 5

    class Config:
        env_file = str(BACKEND_DIR / ".env")
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
