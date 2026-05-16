from uuid import UUID
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project, ProjectRoleSlot, ProjectMember
from app.models.profile import UserProfile
from app.schemas.project import (
    ProjectCreate, ProjectUpdate, ProjectResponse, ProjectCardResponse,
    RoleSlotCreate, RoleSlotResponse, ProjectMemberResponse,
)
from app.services.embedding_service import upsert_project_vector, delete_project_vector
from app.services.notification_service import notify_member_added
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/api/v1/projects", tags=["projects"])


@router.post("", response_model=ProjectResponse, status_code=status.HTTP_201_CREATED)
async def create_project(
    data: ProjectCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    active_count = await db.execute(
        select(func.count()).select_from(Project).where(
            Project.owner_id == current_user.id,
            Project.status.in_(["RECRUITING", "ACTIVE"]),
        )
    )
    if active_count.scalar() >= settings.MAX_ACTIVE_PROJECTS_PER_OWNER:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Maximum 3 active projects allowed")

    start = datetime.strptime(data.start_date, "%d/%m/%Y").replace(tzinfo=timezone.utc)
    end = datetime.strptime(data.end_date, "%d/%m/%Y").replace(tzinfo=timezone.utc)

    if start < datetime.now(timezone.utc):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Start date must be in the future")
    if (end - start).days < 7:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Project must last at least 7 days")

    project = Project(
        owner_id=current_user.id,
        title=data.title,
        field=data.field,
        description=data.description,
        specific_goal=data.specific_goal,
        work_mode=data.work_mode,
        commitment_level=data.commitment_level,
        start_date=start,
        end_date=end,
        max_members=data.max_members,
        additional_requirements=data.additional_requirements,
        member_benefits=data.member_benefits,
    )
    db.add(project)
    await db.flush()

    for slot_data in data.role_slots:
        slot = ProjectRoleSlot(
            project_id=project.id,
            role_name=slot_data.role_name,
            count=slot_data.count,
            filled=0,
            skill_requirements={"requirements": slot_data.skill_requirements} if slot_data.skill_requirements else None,
        )
        db.add(slot)

    owner_member = ProjectMember(
        project_id=project.id,
        user_id=current_user.id,
        role="Project Owner",
        is_owner=True,
    )
    db.add(owner_member)

    await db.flush()

    embedding_id = upsert_project_vector(project)
    if embedding_id:
        project.embedding_id = embedding_id

    return await _build_project_response(db, project)


@router.get("", response_model=list[ProjectResponse])
async def list_my_projects(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    owned = await db.execute(
        select(Project.id).where(Project.owner_id == current_user.id)
    )
    member_of = await db.execute(
        select(ProjectMember.project_id).where(ProjectMember.user_id == current_user.id)
    )
    project_ids = set(r[0] for r in owned.all()) | set(r[0] for r in member_of.all())

    projects = []
    for pid in project_ids:
        project = await db.get(Project, pid)
        if project:
            projects.append(await _build_project_response(db, project))

    return projects


@router.get("/{project_id}", response_model=ProjectResponse)
async def get_project(
    project_id: UUID,
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")
    return await _build_project_response(db, project)


@router.put("/{project_id}", response_model=ProjectResponse)
async def update_project(
    project_id: UUID,
    data: ProjectUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=404, detail="Project not found or not authorized")

    restricted_fields = {"field"}
    date_fields = {"start_date", "end_date"}
    for field_name, value in data.model_dump(exclude_unset=True).items():
        if field_name in restricted_fields:
            continue
        if field_name in date_fields and value is not None:
            value = datetime.strptime(value, "%d/%m/%Y").replace(tzinfo=timezone.utc)
        setattr(project, field_name, value)

    upsert_project_vector(project)

    return await _build_project_response(db, project)


@router.post("/{project_id}/close")
async def close_project(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=404, detail="Project not found or not authorized")

    project.status = "CLOSED"
    delete_project_vector(project)
    return {"message": "Project closed"}


@router.post("/{project_id}/members")
async def add_member(
    project_id: UUID,
    user_id: UUID,
    role: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=404, detail="Project not found or not authorized")

    existing = await db.execute(
        select(ProjectMember).where(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id == user_id,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="User is already a member")

    member = ProjectMember(project_id=project_id, user_id=user_id, role=role)
    db.add(member)

    profile = await db.get(UserProfile, user_id)
    if profile:
        profile.projects_completed = (profile.projects_completed or 0) + 1

    await notify_member_added(db, user_id, project_id)

    if len(project.members) >= project.max_members:
        project.status = "ACTIVE"
        delete_project_vector(project)

    return {"message": "Member added successfully"}


@router.get("/{project_id}/members", response_model=list[ProjectMemberResponse])
async def list_members(
    project_id: UUID,
    db: AsyncSession = Depends(get_db),
):
    members_result = await db.execute(
        select(ProjectMember).where(ProjectMember.project_id == project_id)
    )
    members = members_result.scalars().all()

    response = []
    for m in members:
        member_user = await db.get(User, m.user_id)
        response.append(ProjectMemberResponse(
            id=m.id,
            user_id=m.user_id,
            display_name=member_user.full_name if member_user else "",
            role=m.role,
            is_owner=m.is_owner,
            avatar_url=member_user.avatar_url if member_user else None,
            joined_at=m.joined_at,
        ))

    return response


async def _build_project_response(db: AsyncSession, project: Project) -> ProjectResponse:
    slots_result = await db.execute(
        select(ProjectRoleSlot).where(ProjectRoleSlot.project_id == project.id)
    )
    members_result = await db.execute(
        select(ProjectMember).where(ProjectMember.project_id == project.id)
    )
    members = members_result.scalars().all()

    member_responses = []
    for m in members:
        member_user = await db.get(User, m.user_id)
        member_responses.append(ProjectMemberResponse(
            id=m.id,
            user_id=m.user_id,
            display_name=member_user.full_name if member_user else "",
            role=m.role,
            is_owner=m.is_owner,
            avatar_url=member_user.avatar_url if member_user else None,
            joined_at=m.joined_at,
        ))

    return ProjectResponse(
        id=project.id,
        owner_id=project.owner_id,
        title=project.title,
        field=project.field,
        description=project.description,
        specific_goal=project.specific_goal,
        thumbnail_url=project.thumbnail_url,
        work_mode=project.work_mode,
        commitment_level=project.commitment_level,
        start_date=project.start_date,
        end_date=project.end_date,
        max_members=project.max_members,
        status=project.status,
        additional_requirements=project.additional_requirements,
        member_benefits=project.member_benefits,
        role_slots=[RoleSlotResponse.model_validate(s) for s in slots_result.scalars().all()],
        members=member_responses,
        created_at=project.created_at,
    )
