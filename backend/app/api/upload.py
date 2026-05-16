import uuid
import logging
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.ext.asyncio import AsyncSession
import boto3
from botocore.config import Config

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

router = APIRouter(prefix="/api/v1/upload", tags=["upload"])

ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif"}
MAX_FILE_SIZE = 5 * 1024 * 1024


def _get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=f"{'https' if settings.MINIO_SECURE else 'http'}://{settings.MINIO_ENDPOINT}",
        aws_access_key_id=settings.MINIO_ACCESS_KEY,
        aws_secret_access_key=settings.MINIO_SECRET_KEY,
        config=Config(signature_version="s3v4"),
        region_name="us-east-1",
    )


@router.post("/avatar")
async def upload_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(status_code=400, detail=f"File type {file.content_type} not allowed")

    contents = await file.read()
    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(status_code=400, detail="File too large (max 5MB)")

    ext = file.filename.rsplit(".", 1)[-1] if "." in (file.filename or "") else "png"
    object_name = f"avatars/{current_user.id}/{uuid.uuid4()}.{ext}"

    try:
        client = _get_s3_client()
        client.put_object(
            Bucket=settings.MINIO_BUCKET,
            Key=object_name,
            Body=contents,
            ContentType=file.content_type,
        )
    except Exception as e:
        logger.error(f"Failed to upload avatar: {e}")
        raise HTTPException(status_code=500, detail="Failed to upload file")

    url = f"{'https' if settings.MINIO_SECURE else 'http'}://{settings.MINIO_ENDPOINT}/{settings.MINIO_BUCKET}/{object_name}"
    current_user.avatar_url = url

    return {"avatar_url": url}


@router.post("/thumbnail/{project_id}")
async def upload_project_thumbnail(
    project_id: uuid.UUID,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")

    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(status_code=400, detail=f"File type {file.content_type} not allowed")

    contents = await file.read()
    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(status_code=400, detail="File too large (max 5MB)")

    ext = file.filename.rsplit(".", 1)[-1] if "." in (file.filename or "") else "png"
    object_name = f"thumbnails/{project_id}/{uuid.uuid4()}.{ext}"

    try:
        client = _get_s3_client()
        client.put_object(
            Bucket=settings.MINIO_BUCKET,
            Key=object_name,
            Body=contents,
            ContentType=file.content_type,
        )
    except Exception as e:
        logger.error(f"Failed to upload thumbnail: {e}")
        raise HTTPException(status_code=500, detail="Failed to upload file")

    url = f"{'https' if settings.MINIO_SECURE else 'http'}://{settings.MINIO_ENDPOINT}/{settings.MINIO_BUCKET}/{object_name}"
    project.thumbnail_url = url

    return {"thumbnail_url": url}
