from contextlib import asynccontextmanager
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.database import init_db
#from app.milvus_client import init_milvus_collections
from app.redis_client import get_redis, close_redis
from app.api.auth import router as auth_router
from app.api.profile import router as profile_router
from app.api.project import router as project_router
from app.api.swipe import router as swipe_router
from app.api.task import router as task_router
from app.api.suggestion import router as suggestion_router
from app.api.chat import router as chat_router
from app.api.endpoints.trust import router as trust_router
from app.api.endpoints.fcm import router as fcm_router
from app.api.websocket import handle_presence_websocket, handle_websocket
from app.api.upload import router as upload_router
from app.api.search import router as search_router
from app.api.feedback import router as feedback_router
from app.api.admin import router as project_review_admin_router
from app.api.endpoints.admin import router as user_admin_router
from app.services.fcm_service import init_firebase
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from app.services.task_scheduler import (
    check_deadline_reminders,
    expire_unchecked_tasks,
    auto_confirm_checkouts,
)
from app.services.outbox_service import process_outbox
from app.services.swipe_service import expire_all_queued_items

settings = get_settings()
settings.validate_runtime()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    scheduler = AsyncIOScheduler(timezone="UTC")
    scheduler.add_job(check_deadline_reminders, "interval", hours=1, id="deadline-reminders", replace_existing=True)
    scheduler.add_job(expire_unchecked_tasks, "interval", minutes=15, id="task-expiry", replace_existing=True)
    scheduler.add_job(auto_confirm_checkouts, "interval", minutes=30, id="checkout-confirmation", replace_existing=True)
    scheduler.add_job(process_outbox, "interval", seconds=10, id="outbox", replace_existing=True)
    scheduler.add_job(expire_all_queued_items, "interval", minutes=10, id="queue-expiry", replace_existing=True)
    scheduler.start()
    try:
        pass
        #init_milvus_collections()
    except Exception:
        pass
    try:
        init_firebase()
    except Exception:
        pass
    await get_redis()
    yield
    scheduler.shutdown(wait=False)
    await close_redis()


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials="*" not in settings.CORS_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(profile_router)
app.include_router(project_router)
app.include_router(swipe_router)
app.include_router(task_router)
app.include_router(suggestion_router)
app.include_router(chat_router)
app.include_router(upload_router)
app.include_router(search_router)
app.include_router(trust_router)
app.include_router(fcm_router)
app.include_router(feedback_router)
app.include_router(user_admin_router)
app.include_router(project_review_admin_router)


@app.websocket("/ws/chat/{match_id}")
async def chat_websocket(websocket: WebSocket, match_id: str, token: str = ""):
    await handle_websocket(websocket, match_id, token)


@app.websocket("/ws/presence")
async def presence_websocket(websocket: WebSocket, token: str = ""):
    await handle_presence_websocket(websocket, token)


@app.get("/")
async def root():
    return {"message": "HiBuddy API", "version": settings.APP_VERSION}


@app.get("/health")
async def health():
    return {"status": "healthy"}
