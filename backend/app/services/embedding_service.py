import logging
from typing import Any
from app.config import get_settings
from app.milvus_client import connect_milvus, disconnect_milvus

logger = logging.getLogger(__name__)
settings = get_settings()

_model: Any | None = None


def _get_model() -> Any:
    if not settings.ENABLE_EMBEDDINGS:
        raise RuntimeError("Embeddings are disabled")

    global _model
    if _model is None:
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:
            raise RuntimeError(
                "Embeddings require backend/requirements-ml.txt"
            ) from exc
        _model = SentenceTransformer(settings.SENTENCE_TRANSFORMER_MODEL, local_files_only=True)
    return _model


def build_user_text(profile) -> str:
    parts = [
        profile.display_name or "",
        profile.bio or "",
        ", ".join(r.role_name for r in profile.user.roles),
        ", ".join(f"{s.skill_name}({s.level})" for s in profile.user.skills),
        ", ".join(i.interest_name for i in profile.user.interests),
        profile.user.university or "",
        profile.short_term_goal or "",
    ]
    return " | ".join(filter(None, parts))


def build_project_text(project) -> str:
    parts = [
        project.title or "",
        project.description or "",
        project.specific_goal or "",
        project.field or "",
        ", ".join(slot.role_name for slot in project.role_slots),
        project.work_mode or "",
    ]
    return " | ".join(filter(None, parts))


def encode_text(text: str) -> list[float]:
    if not settings.ENABLE_EMBEDDINGS:
        raise RuntimeError("Embeddings are disabled")
    model = _get_model()
    return model.encode(text).tolist()


def upsert_user_vector(profile) -> int | None:
    try:
        text = build_user_text(profile)
        vector = encode_text(text)
    except Exception as e:
        logger.error(f"Failed to encode user vector: {e}")
        return None

    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("user_profile_vectors")
        collection.load()

        if profile.embedding_id:
            try:
                collection.delete(f"id == {profile.embedding_id}")
            except Exception:
                pass

        data = [{
            "user_id": str(profile.user_id),
            "vector": vector,
            "mode": profile.mode or "BOTH",
            "is_active": not profile.is_hidden,
            "reputation_score": profile.reputation_score or 3.0,
        }]
        result = collection.insert(data)
        collection.flush()
        disconnect_milvus()
        return result.primary_keys[0]
    except Exception as e:
        logger.error(f"Failed to upsert user vector: {e}")
        disconnect_milvus()
        return None


def upsert_project_vector(project) -> int | None:
    try:
        text = build_project_text(project)
        vector = encode_text(text)
    except Exception as e:
        logger.error(f"Failed to encode project vector: {e}")
        return None

    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("project_vectors")
        collection.load()

        if project.embedding_id:
            try:
                collection.delete(f"id == {project.embedding_id}")
            except Exception:
                pass

        data = [{
            "project_id": str(project.id),
            "vector": vector,
            "field": project.field,
            "status": project.status,
            "work_mode": project.work_mode or "ONLINE",
        }]
        result = collection.insert(data)
        collection.flush()
        disconnect_milvus()
        return result.primary_keys[0]
    except Exception as e:
        logger.error(f"Failed to upsert project vector: {e}")
        disconnect_milvus()
        return None


def delete_user_vector(profile) -> None:
    if not profile.embedding_id:
        return
    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("user_profile_vectors")
        collection.load()
        collection.delete(f"id == {profile.embedding_id}")
        disconnect_milvus()
    except Exception as e:
        logger.error(f"Failed to delete user vector: {e}")
        disconnect_milvus()


def delete_project_vector(project) -> None:
    if not project.embedding_id:
        return
    try:
        from pymilvus import Collection
        connect_milvus()
        collection = Collection("project_vectors")
        collection.load()
        collection.delete(f"id == {project.embedding_id}")
        disconnect_milvus()
    except Exception as e:
        logger.error(f"Failed to delete project vector: {e}")
        disconnect_milvus()
