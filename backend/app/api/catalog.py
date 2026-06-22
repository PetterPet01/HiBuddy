from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func
from sqlalchemy.orm import selectinload

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.catalog import RoleCatalog, SkillCatalog, RoleSkillCatalog

router = APIRouter(prefix="/api/v1/catalogs", tags=["catalogs"])


@router.get("/roles")
async def list_roles(
    q: str | None = Query(None, description="Filter by role name"),
    limit: int = Query(200, ge=1, le=500),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    query = select(RoleCatalog).where(RoleCatalog.is_active == True)
    if q:
        query = query.where(RoleCatalog.name.ilike(f"%{q.strip()}%"))
    query = query.order_by(RoleCatalog.name).limit(limit)
    result = await db.execute(query)
    return [
        {"id": str(role.id), "name": role.name, "slug": role.slug}
        for role in result.scalars()
    ]


@router.get("/skills")
async def list_skills(
    q: str | None = Query(None, description="Filter by skill name"),
    limit: int = Query(300, ge=1, le=1000),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    query = select(SkillCatalog).where(SkillCatalog.is_active == True)
    if q:
        query = query.where(SkillCatalog.name.ilike(f"%{q.strip()}%"))
    query = query.order_by(SkillCatalog.name).limit(limit)
    result = await db.execute(query)
    return [
        {"id": str(skill.id), "name": skill.name, "slug": skill.slug}
        for skill in result.scalars()
    ]


@router.get("/roles/{role_id}/skills")
async def list_role_skills(
    role_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(SkillCatalog)
        .join(RoleSkillCatalog, RoleSkillCatalog.skill_id == SkillCatalog.id)
        .where(RoleSkillCatalog.role_id == role_id, SkillCatalog.is_active == True)
        .order_by(SkillCatalog.name)
    )
    return [
        {"id": str(skill.id), "name": skill.name, "slug": skill.slug}
        for skill in result.scalars()
    ]
