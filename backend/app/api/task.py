from uuid import UUID
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project, ProjectMember
from app.models.task import Task, TaskCheckoutHistory
from app.models.profile import UserProfile
from app.schemas.task import (
    TaskCreate, TaskUpdate, TaskStatusUpdate, TaskCheckoutOverride,
    TaskResponse, DashboardResponse, MemberStatResponse, EvaluationCreate, EvaluationResponse,
)
from app.services.notification_service import notify_task_assigned, notify_deadline_reminder, notify_checkout
from app.services.task_scheduler import _recalculate_user_score
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/api/v1", tags=["tasks"])


@router.post("/projects/{project_id}/tasks", response_model=TaskResponse, status_code=status.HTTP_201_CREATED)
async def create_task(
    project_id: UUID,
    data: TaskCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only project owner can create tasks")

    try:
        assignee_uuid = UUID(data.assignee_id)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail="Invalid assignee id") from exc
    is_owner_assignee = assignee_uuid == project.owner_id
    member_result = await db.execute(
        select(ProjectMember).where(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id == assignee_uuid,
        )
    )
    if not is_owner_assignee and not member_result.scalar_one_or_none():
        raise HTTPException(status_code=400, detail="Assignee must be a member of the project")

    try:
        start = datetime.strptime(data.start_date, "%d/%m/%Y").replace(tzinfo=timezone.utc)
        deadline = datetime.strptime(data.deadline, "%d/%m/%Y").replace(tzinfo=timezone.utc)
    except ValueError as exc:
        raise HTTPException(status_code=422, detail="Dates must use DD/MM/YYYY") from exc

    if start < project.start_date or start > project.end_date:
        raise HTTPException(status_code=400, detail="Task start date must be within the project timeline")
    if deadline < start:
        raise HTTPException(status_code=400, detail="Deadline must be after the start date")
    if deadline < datetime.now(timezone.utc) + timedelta(days=1):
        raise HTTPException(status_code=400, detail="Deadline must be at least tomorrow")
    if deadline > project.end_date:
        raise HTTPException(status_code=400, detail="Deadline must not exceed project end date")

    task = Task(
        project_id=project_id,
        assignee_id=assignee_uuid,
        creator_id=current_user.id,
        title=data.title,
        description=data.description,
        role_related=data.role_related,
        priority=data.priority,
        status="TODO",
        start_date=start if start else datetime.now(timezone.utc),
        deadline=deadline,
        tag=data.tag,
    )
    db.add(task)
    await db.flush()

    await notify_task_assigned(db, task)

    return await _build_task_response(db, task)


@router.get("/projects/{project_id}/tasks", response_model=list[TaskResponse])
async def list_tasks(
    project_id: UUID,
    status_filter: str | None = Query(None, alias="status"),
    assignee_id: UUID | None = Query(None, alias="assignee_id"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")

    member_check = await db.execute(
        select(ProjectMember).where(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id == current_user.id,
        )
    )
    if project.owner_id != current_user.id and not member_check.scalar_one_or_none():
        raise HTTPException(status_code=403, detail="Not a member of this project")

    query = select(Task).where(Task.project_id == project_id)
    if status_filter:
        query = query.where(Task.status == status_filter)
    if assignee_id:
        query = query.where(Task.assignee_id == assignee_id)
    query = query.order_by(Task.priority.desc(), Task.deadline.asc())

    tasks = (await db.execute(query)).scalars().all()
    return [await _build_task_response(db, t) for t in tasks]


@router.put("/tasks/{task_id}", response_model=TaskResponse)
async def update_task(
    task_id: UUID,
    data: TaskUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only project owner can update tasks")

    prospective_deadline = task.deadline
    prospective_assignee = task.assignee_id
    for field, value in data.model_dump(exclude_unset=True).items():
        if value is not None:
            if field == "deadline":
                try:
                    value = datetime.strptime(value, "%d/%m/%Y").replace(tzinfo=timezone.utc)
                except ValueError as exc:
                    raise HTTPException(status_code=422, detail="Deadline must use DD/MM/YYYY") from exc
                prospective_deadline = value
            if field == "assignee_id":
                try:
                    value = UUID(value)
                except ValueError as exc:
                    raise HTTPException(status_code=422, detail="Invalid assignee id") from exc
                prospective_assignee = value
            setattr(task, field, value)
    if prospective_deadline <= task.start_date or prospective_deadline > project.end_date:
        raise HTTPException(status_code=400, detail="Deadline must be inside the project timeline")
    member = await db.scalar(
        select(ProjectMember.id).where(
            ProjectMember.project_id == project.id,
            ProjectMember.user_id == prospective_assignee,
        )
    )
    if prospective_assignee != project.owner_id and not member:
        raise HTTPException(status_code=400, detail="Assignee must be a project member")

    return await _build_task_response(db, task)


@router.patch("/tasks/{task_id}/status")
async def update_task_status(
    task_id: UUID,
    data: TaskStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    if data.status == "IN_PROGRESS":
        if task.assignee_id != current_user.id:
            raise HTTPException(status_code=403, detail="Only assignee can start task")
        if task.status != "TODO":
            raise HTTPException(status_code=400, detail="Task must be in TODO status")

    elif data.status == "DONE_REVIEW":
        if task.assignee_id != current_user.id:
            raise HTTPException(status_code=403, detail="Only assignee can submit task")
        if task.status != "IN_PROGRESS":
            raise HTTPException(status_code=400, detail="Task must be in progress")

    elif data.status == "CLOSED":
        project = await db.get(Project, task.project_id)
        if not project or project.owner_id != current_user.id:
            raise HTTPException(status_code=403, detail="Only owner can close tasks")
        if task.status != "DONE_REVIEW":
            raise HTTPException(status_code=400, detail="Task must be in review before closing")

    previous_status = task.status
    task.status = data.status
    if previous_status != data.status:
        db.add(TaskCheckoutHistory(
            task_id=task.id,
            action="STATUS_UPDATE",
            actor_id=current_user.id,
            previous_status=previous_status,
            new_status=data.status,
            notes=f"Status changed from {previous_status} to {data.status}",
        ))
    if data.status == "CLOSED":
        task.checkout_confirmed_at = datetime.now(timezone.utc)
        await _recalculate_user_score(db, task.assignee_id)
    return {"message": f"Task status updated to {data.status}"}


@router.post("/tasks/{task_id}/checkout")
async def checkout_task(
    task_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task or task.assignee_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only assignee can checkout")

    if task.status != "IN_PROGRESS":
        raise HTTPException(status_code=400, detail="Task must be in progress")

    now = datetime.now(timezone.utc)
    deadline = task.deadline

    if now.date() < deadline.date():
        checkout_status = "EARLY"
    elif now.date() == deadline.date():
        checkout_status = "ON_TIME"
    else:
        checkout_status = "LATE"

    task.status = "DONE_REVIEW"
    task.checkout_at = now
    task.checkout_status = checkout_status

    history = TaskCheckoutHistory(
        task_id=task.id,
        action="CHECKOUT",
        actor_id=current_user.id,
        previous_status="IN_PROGRESS",
        new_status="DONE_REVIEW",
        notes=f"Checkout status: {checkout_status}",
    )
    db.add(history)

    await notify_checkout(db, task, checkout_status, current_user.full_name)

    return {"message": "Checkout recorded", "checkout_status": checkout_status}


@router.post("/tasks/{task_id}/confirm-checkout")
async def confirm_checkout(
    task_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only owner can confirm")

    if task.status != "DONE_REVIEW":
        raise HTTPException(status_code=400, detail="Task must be in review")

    task.status = "CLOSED"
    task.checkout_confirmed_at = datetime.now(timezone.utc)

    history = TaskCheckoutHistory(
        task_id=task.id,
        action="CONFIRM",
        actor_id=current_user.id,
        previous_status="DONE_REVIEW",
        new_status="CLOSED",
        notes="Owner confirmed checkout",
    )
    db.add(history)

    await _recalculate_user_score(db, task.assignee_id)

    return {"message": "Checkout confirmed"}


@router.post("/tasks/{task_id}/override")
async def override_checkout(
    task_id: UUID,
    data: TaskCheckoutOverride,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only owner can override")

    old_status = task.checkout_status
    task.checkout_status = data.checkout_status
    task.status = "CLOSED"
    task.checkout_confirmed_at = datetime.now(timezone.utc)

    history = TaskCheckoutHistory(
        task_id=task.id,
        action="OVERRIDE",
        actor_id=current_user.id,
        previous_status=old_status or "PENDING",
        new_status=data.checkout_status,
        notes=data.notes or "Owner override",
    )
    db.add(history)

    await _recalculate_user_score(db, task.assignee_id)

    return {"message": "Checkout overridden", "new_status": data.checkout_status}


@router.delete("/tasks/{task_id}")
async def delete_task(
    task_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only owner can delete tasks")

    await db.delete(task)
    return {"message": "Task deleted"}


@router.get("/projects/{project_id}/dashboard", response_model=DashboardResponse)
async def get_dashboard(
    project_id: UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    project = await db.get(Project, project_id)
    if not project:
        raise HTTPException(status_code=404, detail="Project not found")

    member_check = await db.execute(
        select(ProjectMember).where(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id == current_user.id,
        )
    )
    if project.owner_id != current_user.id and not member_check.scalar_one_or_none():
        raise HTTPException(status_code=403, detail="Not a member of this project")

    members_result = await db.execute(
        select(ProjectMember).where(ProjectMember.project_id == project_id)
    )
    members = members_result.scalars().all()

    member_stats = []
    total_tasks = 0

    for member in members:
        member_user = await db.get(User, member.user_id)
        tasks_result = await db.execute(
            select(Task).where(
                Task.project_id == project_id,
                Task.assignee_id == member.user_id,
            )
        )
        tasks = tasks_result.scalars().all()
        total_tasks += len(tasks)

        early = sum(1 for t in tasks if t.checkout_status == "EARLY")
        on_time = sum(1 for t in tasks if t.checkout_status == "ON_TIME")
        late = sum(1 for t in tasks if t.checkout_status in ("LATE", "LATE_CHECKOUT", "NOT_COMPLETED"))
        in_progress = sum(1 for t in tasks if t.status == "IN_PROGRESS")
        todo = sum(1 for t in tasks if t.status == "TODO")

        member_stats.append(MemberStatResponse(
            user_id=member.user_id,
            display_name=member_user.full_name if member_user else "",
            role=member.role,
            total_tasks=len(tasks),
            early=early,
            on_time=on_time,
            late=late,
            in_progress=in_progress,
            todo=todo,
        ))

    return DashboardResponse(
        project_id=project_id,
        project_title=project.title,
        total_tasks=total_tasks,
        total_members=len(members),
        member_stats=member_stats,
    )


@router.post("/projects/{project_id}/evaluate/{member_id}", response_model=EvaluationResponse, status_code=status.HTTP_201_CREATED)
async def evaluate_member(
    project_id: UUID,
    member_id: UUID,
    data: EvaluationCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    # Numeric owner-to-member evaluations are disabled for this project.
    # Reputation is calculated from task completion status, while qualitative feedback
    # is handled through the anonymous feedback flow.
    raise HTTPException(status_code=410, detail="Numeric project evaluations are disabled")


async def _build_task_response(db: AsyncSession, task: Task) -> TaskResponse:
    assignee = await db.get(User, task.assignee_id)
    return TaskResponse(
        id=task.id,
        project_id=task.project_id,
        assignee_id=task.assignee_id,
        creator_id=task.creator_id,
        title=task.title,
        description=task.description,
        role_related=task.role_related,
        priority=task.priority,
        status=task.status,
        start_date=task.start_date,
        deadline=task.deadline,
        tag=task.tag,
        checkout_at=task.checkout_at,
        checkout_confirmed_at=task.checkout_confirmed_at,
        checkout_status=task.checkout_status,
        assignee_name=assignee.full_name if assignee else "",
        assignee_avatar=assignee.avatar_url if assignee else None,
        created_at=task.created_at,
    )
