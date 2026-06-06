import asyncio
import io
import logging
import uuid

import boto3
from botocore.config import Config
from fastapi import APIRouter, Depends, File, HTTPException, Request, UploadFile
from fastapi.responses import StreamingResponse
from PIL import Image, ImageOps, UnidentifiedImageError
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.dependencies import get_current_user
from app.database import get_db
from app.models.project import Project
from app.models.user import User

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter(prefix="/api/v1/upload", tags=["upload"])

MAX_FILE_SIZE = 5 * 1024 * 1024
MAX_PIXELS = 25_000_000


def _get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=f"{'https' if settings.MINIO_SECURE else 'http'}://{settings.MINIO_ENDPOINT}",
        aws_access_key_id=settings.MINIO_ACCESS_KEY,
        aws_secret_access_key=settings.MINIO_SECRET_KEY,
        config=Config(signature_version="s3v4"),
        region_name="us-east-1",
    )


def _ensure_bucket(client) -> None:
    try:
        client.head_bucket(Bucket=settings.MINIO_BUCKET)
    except Exception:
        client.create_bucket(Bucket=settings.MINIO_BUCKET)


def _normalize_image(
    contents: bytes, max_size: tuple[int, int], crop_to_size: bool = False
) -> bytes:
    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(status_code=413, detail="File too large (max 5MB)")
    try:
        with Image.open(io.BytesIO(contents)) as source:
            if source.width * source.height > MAX_PIXELS:
                raise HTTPException(status_code=400, detail="Image dimensions are too large")
            source.verify()
        with Image.open(io.BytesIO(contents)) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")
            if crop_to_size:
                image = ImageOps.fit(
                    image,
                    max_size,
                    method=Image.Resampling.LANCZOS,
                    centering=(0.5, 0.5),
                )
            else:
                image.thumbnail(max_size, Image.Resampling.LANCZOS)
            output = io.BytesIO()
            image.save(output, format="JPEG", quality=88, optimize=True)
            return output.getvalue()
    except (UnidentifiedImageError, OSError) as exc:
        raise HTTPException(status_code=400, detail="Invalid image file") from exc


def _public_url(request: Request, object_name: str) -> str:
    base = settings.MEDIA_PUBLIC_BASE_URL.rstrip("/")
    if not base:
        base = f"{str(request.base_url).rstrip('/')}/api/v1/upload/media"
    return f"{base}/{object_name}"


def _object_name_from_url(url: str | None) -> str | None:
    if not url or "/media/" not in url:
        return None
    return url.split("/media/", 1)[1]


async def _put_image(
    request: Request,
    contents: bytes,
    prefix: str,
    max_size: tuple[int, int],
    previous_url: str | None = None,
    crop_to_size: bool = False,
) -> str:
    normalized = await asyncio.to_thread(
        _normalize_image, contents, max_size, crop_to_size
    )
    object_name = f"{prefix}/{uuid.uuid4()}.jpg"
    client = _get_s3_client()

    def upload() -> None:
        _ensure_bucket(client)
        client.put_object(
            Bucket=settings.MINIO_BUCKET,
            Key=object_name,
            Body=normalized,
            ContentType="image/jpeg",
            CacheControl="public, max-age=86400",
        )
        previous = _object_name_from_url(previous_url)
        if previous and previous != object_name:
            try:
                client.delete_object(Bucket=settings.MINIO_BUCKET, Key=previous)
            except Exception:
                logger.warning("Could not delete replaced media object %s", previous)

    await asyncio.to_thread(upload)
    return _public_url(request, object_name)


@router.get("/media/{object_name:path}", include_in_schema=False)
async def get_media(object_name: str):
    client = _get_s3_client()
    try:
        response = await asyncio.to_thread(
            client.get_object, Bucket=settings.MINIO_BUCKET, Key=object_name
        )
    except Exception as exc:
        raise HTTPException(status_code=404, detail="Media not found") from exc
    return StreamingResponse(
        response["Body"],
        media_type=response.get("ContentType", "application/octet-stream"),
        headers={"Cache-Control": "public, max-age=86400"},
    )


@router.post("/avatar")
async def upload_avatar(
    request: Request,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    contents = await file.read(MAX_FILE_SIZE + 1)
    try:
        url = await _put_image(
            request,
            contents,
            f"avatars/{current_user.id}",
            (1024, 1024),
            current_user.avatar_url,
            crop_to_size=True,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Avatar upload failed")
        raise HTTPException(status_code=500, detail="Failed to upload avatar") from exc
    current_user.avatar_url = url
    return {"avatar_url": url}


@router.delete("/avatar")
async def delete_avatar(
    current_user: User = Depends(get_current_user),
):
    previous = _object_name_from_url(current_user.avatar_url)
    if previous:
        client = _get_s3_client()
        try:
            await asyncio.to_thread(
                client.delete_object, Bucket=settings.MINIO_BUCKET, Key=previous
            )
        except Exception as exc:
            logger.exception("Avatar deletion failed")
            raise HTTPException(status_code=500, detail="Failed to delete avatar") from exc
    current_user.avatar_url = None
    return {"message": "Avatar removed"}


@router.post("/student-card")
async def upload_student_card(
    request: Request,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
):
    contents = await file.read(MAX_FILE_SIZE + 1)
    try:
        url = await _put_image(
            request,
            contents,
            f"student-cards/{current_user.id}",
            (1600, 1200),
            current_user.student_card_image_url,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Student card upload failed")
        raise HTTPException(status_code=500, detail="Failed to upload student card") from exc
    current_user.student_card_image_url = url
    return {"image_url": url}


@router.post("/thumbnail/{project_id}")
async def upload_project_thumbnail(
    request: Request,
    project_id: uuid.UUID,
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized")
    contents = await file.read(MAX_FILE_SIZE + 1)
    try:
        url = await _put_image(
            request,
            contents,
            f"thumbnails/{project.id}",
            (1600, 900),
            project.thumbnail_url,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.exception("Thumbnail upload failed")
        raise HTTPException(status_code=500, detail="Failed to upload thumbnail") from exc
    project.thumbnail_url = url
    return {"thumbnail_url": url}
