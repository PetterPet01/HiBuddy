from uuid import UUID
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.database import get_db
from app.core.dependencies import get_current_user
from app.models.user import User
from app.models.project import Project, ProjectMember
from app.models.task import Task, TaskAssignment, TaskCheckoutHistory
from app.models.profile import UserProfile
from app.schemas.task import (
    TaskAttachmentPayload, TaskCreate, TaskSubmission, TaskUpdate, TaskStatusUpdate,
    TaskCheckoutOverride, TaskReject, TaskAssigneeSummary,
    TaskResponse, DashboardResponse, MemberStatResponse, EvaluationCreate, EvaluationResponse,
)
from app.services.notification_service import (
    notify_task_assigned, notify_deadline_reminder, notify_checkout, notify_task_rejected,
)
from app.services.task_scheduler import _recalculate_user_score
from app.config import get_settings

settings = get_settings()
router = APIRouter(prefix="/api/v1", tags=["tasks"])


def _normalize_attachment_payloads(
    attachments: list[TaskAttachmentPayload] | None,
) -> list[dict]:
    return [
        {
            "url": item.url,
            "name": item.name,
            "content_type": item.content_type,
        }
        for item in (attachments or [])
    ]


def _task_attachment_state(task: Task) -> dict:
    payload = task.attachment_urls if isinstance(task.attachment_urls, dict) else {}
    legacy_attachments = task.attachment_urls if isinstance(task.attachment_urls, list) else None
    return {
        "task_attachments": payload.get("task_attachments") or legacy_attachments or [],
        "submission_note": payload.get("submission_note"),
        "submission_links": payload.get("submission_links") or [],
        "submission_attachments": payload.get("submission_attachments") or [],
    }


def _save_task_attachment_state(
    task: Task,
    *,
    task_attachments: list[dict] | None = None,
    submission_note: str | None = None,
    submission_links: list[str] | None = None,
    submission_attachments: list[dict] | None = None,
    replace_submission_note: bool = False,
    replace_submission_links: bool = False,
    replace_submission_attachments: bool = False,
) -> None:
    current = _task_attachment_state(task)
    task.attachment_urls = {
        "task_attachments": current["task_attachments"] if task_attachments is None else task_attachments,
        "submission_note": submission_note if replace_submission_note else current["submission_note"],
        "submission_links": submission_links if replace_submission_links else current["submission_links"],
        "submission_attachments": submission_attachments if replace_submission_attachments else current["submission_attachments"],
    }


def _assignee_ids(task: Task) -> list[UUID]:
    return [a.assignee_id for a in task.assignments]


def _is_assignee(task: Task, user_id: UUID) -> bool:
    return any(a.assignee_id == user_id for a in task.assignments)


async def _validate_assignees(
    db: AsyncSession, project: Project, raw_ids: list[str]
) -> list[UUID]:
    parsed: list[UUID] = []
    seen: set[UUID] = set()
    for raw in raw_ids:
        try:
            uid = UUID(raw)
        except (ValueError, AttributeError) as exc:
            raise HTTPException(status_code=422, detail="Invalid assignee id") from exc
        if uid in seen:
            continue
        seen.add(uid)
        parsed.append(uid)
    if not parsed:
        raise HTTPException(status_code=400, detail="At least one assignee is required")

    member_rows = await db.execute(
        select(ProjectMember.user_id).where(ProjectMember.project_id == project.id)
    )
    allowed = {row[0] for row in member_rows.all()}
    allowed.add(project.owner_id)
    for uid in parsed:
        if uid not in allowed:
            raise HTTPException(status_code=400, detail="Assignee must be a member of the project")
    return parsed


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

    assignee_ids = await _validate_assignees(db, project, data.assignee_ids)

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
    task.assignments = [TaskAssignment(assignee_id=uid) for uid in assignee_ids]
    _save_task_attachment_state(
        task,
        task_attachments=_normalize_attachment_payloads(data.attachment_urls),
        submission_note=None,
        submission_links=[],
        submission_attachments=[],
        replace_submission_note=True,
        replace_submission_links=True,
        replace_submission_attachments=True,
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
        query = query.where(
            Task.id.in_(
                select(TaskAssignment.task_id).where(TaskAssignment.assignee_id == assignee_id)
            )
        )
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

    prospective_start_date = task.start_date
    prospective_deadline = task.deadline
    new_assignee_ids: list[UUID] | None = None
    for field, value in data.model_dump(exclude_unset=True).items():
        if value is not None:
            if field == "start_date":
                try:
                    value = datetime.strptime(value, "%d/%m/%Y").replace(tzinfo=timezone.utc)
                except ValueError as exc:
                    raise HTTPException(status_code=422, detail="Start date must use DD/MM/YYYY") from exc
                prospective_start_date = value
            if field == "deadline":
                try:
                    value = datetime.strptime(value, "%d/%m/%Y").replace(tzinfo=timezone.utc)
                except ValueError as exc:
                    raise HTTPException(status_code=422, detail="Deadline must use DD/MM/YYYY") from exc
                prospective_deadline = value
            if field == "assignee_ids":
                new_assignee_ids = await _validate_assignees(db, project, value)
                continue
            if field == "attachment_urls":
                _save_task_attachment_state(
                    task,
                    task_attachments=_normalize_attachment_payloads(
                        [TaskAttachmentPayload.model_validate(item) for item in value]
                    ),
                )
                continue
            setattr(task, field, value)
    if prospective_start_date < project.start_date or prospective_start_date > project.end_date:
        raise HTTPException(status_code=400, detail="Task start date must be within the project timeline")
    if prospective_deadline <= prospective_start_date or prospective_deadline > project.end_date:
        raise HTTPException(status_code=400, detail="Deadline must be inside the project timeline")

    if new_assignee_ids is not None:
        existing = {a.assignee_id: a for a in task.assignments}
        target = set(new_assignee_ids)
        for uid, assignment in list(existing.items()):
            if uid not in target:
                task.assignments.remove(assignment)
        for uid in new_assignee_ids:
            if uid not in existing:
                task.assignments.append(TaskAssignment(assignee_id=uid))

    await db.flush()
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
        if not _is_assignee(task, current_user.id):
            raise HTTPException(status_code=403, detail="Only an assignee can start task")
        if task.status != "TODO":
            raise HTTPException(status_code=400, detail="Task must be in TODO status")

    elif data.status == "DONE_REVIEW":
        if not _is_assignee(task, current_user.id):
            raise HTTPException(status_code=403, detail="Only an assignee can submit task")
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
        for uid in _assignee_ids(task):
            await _recalculate_user_score(db, uid)
    return {"message": f"Task status updated to {data.status}"}


@router.post("/tasks/{task_id}/checkout")
async def checkout_task(
    task_id: UUID,
    data: TaskSubmission | None = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task or not _is_assignee(task, current_user.id):
        raise HTTPException(status_code=403, detail="Only an assignee can checkout")

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
    _save_task_attachment_state(
        task,
        submission_note=data.submission_note.strip() if data and data.submission_note else None,
        submission_links=[link.strip() for link in (data.submission_links if data else []) if link.strip()],
        submission_attachments=_normalize_attachment_payloads(data.submission_attachments if data else []),
        replace_submission_note=True,
        replace_submission_links=True,
        replace_submission_attachments=True,
    )

    history = TaskCheckoutHistory(
        task_id=task.id,
        action="CHECKOUT",
        actor_id=current_user.id,
        previous_status="IN_PROGRESS",
        new_status="DONE_REVIEW",
        notes=f"Checkout status: {checkout_status}. Submission received for review.",
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

    for uid in _assignee_ids(task):
        await _recalculate_user_score(db, uid)

    return {"message": "Checkout confirmed"}


@router.post("/tasks/{task_id}/reject")
async def reject_task(
    task_id: UUID,
    data: TaskReject | None = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    task = await db.get(Task, task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    project = await db.get(Project, task.project_id)
    if not project or project.owner_id != current_user.id:
        raise HTTPException(status_code=403, detail="Only owner can reject tasks")

    if task.status != "DONE_REVIEW":
        raise HTTPException(status_code=400, detail="Task must be in review to reject")

    notes = (data.notes.strip() if data and data.notes else None)
    previous_status = task.status
    task.status = "IN_PROGRESS"
    task.checkout_at = None
    task.checkout_status = None

    db.add(TaskCheckoutHistory(
        task_id=task.id,
        action="REJECT",
        actor_id=current_user.id,
        previous_status=previous_status,
        new_status="IN_PROGRESS",
        notes=notes or "Owner requested changes",
    ))

    await notify_task_rejected(db, task, notes)

    return {"message": "Task returned to assignee for revision"}


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

    for uid in _assignee_ids(task):
        await _recalculate_user_score(db, uid)

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

    # Tasks for this project, with assignee ids loaded.
    tasks_result = await db.execute(
        select(Task).where(Task.project_id == project_id)
    )
    project_tasks = tasks_result.scalars().all()
    tasks_by_user: dict[UUID, list[Task]] = {}
    for task in project_tasks:
        for uid in _assignee_ids(task):
            tasks_by_user.setdefault(uid, []).append(task)

    member_stats = []
    seen_users: set[UUID] = set()
    total_tasks = len(project_tasks)

    def _stat_for(user_id: UUID, display_name: str, role: str) -> MemberStatResponse:
        tasks = tasks_by_user.get(user_id, [])
        early = sum(1 for t in tasks if t.checkout_status == "EARLY")
        on_time = sum(1 for t in tasks if t.checkout_status == "ON_TIME")
        late = sum(1 for t in tasks if t.checkout_status in ("LATE", "LATE_CHECKOUT", "NOT_COMPLETED"))
        in_progress = sum(1 for t in tasks if t.status == "IN_PROGRESS")
        todo = sum(1 for t in tasks if t.status == "TODO")
        return MemberStatResponse(
            user_id=user_id,
            display_name=display_name,
            role=role,
            total_tasks=len(tasks),
            early=early,
            on_time=on_time,
            late=late,
            in_progress=in_progress,
            todo=todo,
        )

    # Owner can also be a task assignee, so include them in the breakdown.
    owner_user = await db.get(User, project.owner_id)
    member_stats.append(_stat_for(project.owner_id, owner_user.full_name if owner_user else "", "Owner"))
    seen_users.add(project.owner_id)

    for member in members:
        if member.user_id in seen_users:
            continue
        seen_users.add(member.user_id)
        member_user = await db.get(User, member.user_id)
        member_stats.append(_stat_for(
            member.user_id,
            member_user.full_name if member_user else "",
            member.role,
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
    attachments = _task_attachment_state(task)
    assignee_summaries: list[TaskAssigneeSummary] = []
    for assignment in task.assignments:
        user = await db.get(User, assignment.assignee_id)
        assignee_summaries.append(TaskAssigneeSummary(
            user_id=assignment.assignee_id,
            display_name=user.full_name if user else None,
            avatar_url=user.avatar_url if user else None,
        ))
    primary = assignee_summaries[0] if assignee_summaries else None
    return TaskResponse(
        id=task.id,
        project_id=task.project_id,
        assignee_ids=[s.user_id for s in assignee_summaries],
        assignees=assignee_summaries,
        creator_id=task.creator_id,
        title=task.title,
        description=task.description,
        role_related=task.role_related,
        priority=task.priority,
        status=task.status,
        start_date=task.start_date,
        deadline=task.deadline,
        tag=task.tag,
        attachment_urls=[TaskAttachmentPayload.model_validate(item) for item in attachments["task_attachments"]],
        submission_note=attachments["submission_note"],
        submission_links=attachments["submission_links"],
        submission_attachments=[TaskAttachmentPayload.model_validate(item) for item in attachments["submission_attachments"]],
        checkout_at=task.checkout_at,
        checkout_confirmed_at=task.checkout_confirmed_at,
        checkout_status=task.checkout_status,
        assignee_name=primary.display_name if primary else "",
        assignee_avatar=primary.avatar_url if primary else None,
        created_at=task.created_at,
    )

