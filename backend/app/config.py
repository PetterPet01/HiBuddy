from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
from pathlib import Path


BACKEND_DIR = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(BACKEND_DIR / ".env"),
        env_file_encoding="utf-8",
    )

    APP_NAME: str = "HiBuddy"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = True
    ENVIRONMENT: str = "development"
    SECRET_KEY: str = "change-me-in-production-use-a-strong-random-secret-key"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    REFRESH_TOKEN_SHORT_DAYS: int = 1

    DATABASE_URL: str = "postgresql+asyncpg://hibuddy:hibuddy_secret@localhost:5433/hibuddy"
    DATABASE_URL_SYNC: str = "postgresql://hibuddy:hibuddy_secret@localhost:5433/hibuddy"

    REDIS_URL: str = "redis://localhost:6379/0"

    MILVUS_HOST: str = "localhost"
    MILVUS_PORT: int = 19530

    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "hibuddy"
    MINIO_SECURE: bool = False
    MEDIA_PUBLIC_BASE_URL: str = ""

    GOOGLE_CLIENT_ID: str = ""
    GOOGLE_CLIENT_SECRET: str = ""

    CORS_ORIGINS: list[str] = ["http://localhost:3000"]

    SMTP_HOST: str = ""
    SMTP_PORT: int = 587
    SMTP_USERNAME: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM_EMAIL: str = "noreply@hibuddy.local"
    SMTP_USE_TLS: bool = True
    AUTH_CODE_TTL_MINUTES: int = 15
    AUTH_CODE_RESEND_SECONDS: int = 60
    AUTH_CODE_MAX_ATTEMPTS: int = 5

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

    MISTRAL_API_KEY: str = ""
    MISTRAL_MODEL: str = "mistral-medium-latest"
    MISTRAL_BASE_URL: str = "https://api.mistral.ai/v1"

    def validate_runtime(self) -> None:
        if self.ENVIRONMENT.lower() == "production":
            if self.DEBUG:
                raise RuntimeError("DEBUG must be false in production")
            if self.SECRET_KEY.startswith("change-me") or len(self.SECRET_KEY) < 32:
                raise RuntimeError("A strong SECRET_KEY is required in production")
            if "*" in self.CORS_ORIGINS:
                raise RuntimeError("Wildcard CORS is not allowed in production")
            if not self.SMTP_HOST:
                raise RuntimeError("SMTP_HOST is required in production")

@lru_cache()
def get_settings() -> Settings:
    return Settings()
