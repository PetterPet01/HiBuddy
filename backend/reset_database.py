#!/usr/bin/env python3
"""Reset selected HiBuddy database domains and repopulate seed data.

Examples:
    python backend/reset_database.py --reset messages --yes
    python backend/reset_database.py --reset matches --yes
    python backend/reset_database.py --reset all --keep users --yes
    python backend/reset_database.py --reset projects --no-seed --yes

The script is intentionally conservative: it prints the effective reset plan
unless --yes is provided.
"""

from __future__ import annotations

import argparse
import asyncio
from collections.abc import Awaitable, Callable, Iterable
from urllib.parse import urlsplit, urlunsplit

from sqlalchemy import delete, func, inspect, literal, select, text
from sqlalchemy.engine import Connection
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.schema import CreateColumn
from sqlalchemy.sql.schema import Column, Table

from app.config import get_settings
from app.database import Base
from app.models.chat import Chat, CourseSuggestion, Message, Notification, RefreshToken
from app.models.fcm_token import FCMToken
from app.models.feedback import AnonymousFeedback
from app.models.profile import (
    UserCompletedCourse,
    UserInterest,
    UserProfile,
    UserRole,
    UserSkill,
)
from app.models.project import Project, ProjectMember, ProjectRoleSlot
from app.models.swipe import Match, SwipeAction
from app.models.task import ProjectEvaluation, Task, TaskCheckoutHistory
from app.models.trust_safety import Report, UserBlock
from app.models.user import User
from seed_data import (
    CH1,
    CH2,
    CH3,
    CH4,
    P1,
    P2,
    P3,
    P4,
    P5,
    U1,
    U2,
    U3,
    U4,
    U5,
    U6,
    U7,
    U8,
    _seed_evaluations,
    _seed_matches_and_chats,
    _seed_members,
    _seed_messages,
    _seed_notifications,
    _seed_projects,
    _seed_swipes,
    _seed_tasks,
    _seed_users,
)


Model = type[Base]
SeedStep = tuple[str, Callable[[AsyncSession], Awaitable[None]]]

BASE_DOMAINS = (
    "users",
    "projects",
    "tasks",
    "swipes",
    "matches",
    "messages",
    "notifications",
    "feedback",
)
DOMAIN_CHOICES = (*BASE_DOMAINS, "all")

DOMAIN_DESCRIPTIONS = {
    "users": "Users, profiles, skills, interests, completed courses, auth/session rows, FCM tokens, blocks, and reports. Forces all seeded app data to reset.",
    "projects": "Projects, role slots, members, tasks, project evaluations, and anonymous feedback. Forces swipes, matches, messages, and notifications to reset.",
    "tasks": "Tasks and task checkout history. Also resets notifications because seeded notifications reference tasks.",
    "swipes": "Swipe actions used by discovery and match scenarios.",
    "matches": "Swipes, matches, chats, messages, and notifications.",
    "messages": "Chat messages only. Requires existing seeded chats and users unless matches/users are reset too.",
    "notifications": "Notification rows only.",
    "feedback": "Anonymous peer feedback rows and analyzed weakness payloads.",
}

RESET_IMPLICATIONS = {
    "users": {"projects", "tasks", "swipes", "matches", "messages", "notifications", "feedback"},
    "projects": {"tasks", "swipes", "matches", "messages", "notifications", "feedback"},
    "tasks": {"notifications"},
    "matches": {"swipes", "messages", "notifications"},
}

TABLES_BY_DOMAIN: dict[str, tuple[Model, ...]] = {
    "messages": (Message,),
    "matches": (Chat, Match),
    "swipes": (SwipeAction,),
    "notifications": (Notification,),
    "feedback": (AnonymousFeedback,),
    "tasks": (TaskCheckoutHistory, Task),
    "projects": (ProjectEvaluation, ProjectMember, ProjectRoleSlot, Project),
    "users": (
        CourseSuggestion,
        RefreshToken,
        FCMToken,
        UserBlock,
        Report,
        UserCompletedCourse,
        UserInterest,
        UserSkill,
        UserRole,
        UserProfile,
        User,
    ),
}

DELETE_ORDER: tuple[Model, ...] = (
    Message,
    Chat,
    Notification,
    AnonymousFeedback,
    CourseSuggestion,
    RefreshToken,
    FCMToken,
    UserBlock,
    Report,
    TaskCheckoutHistory,
    ProjectEvaluation,
    Task,
    Match,
    SwipeAction,
    ProjectMember,
    ProjectRoleSlot,
    Project,
    UserCompletedCourse,
    UserInterest,
    UserSkill,
    UserRole,
    UserProfile,
    User,
)

SEED_USER_IDS = (U1, U2, U3, U4, U5, U6, U7, U8)
SEED_PROJECT_IDS = (P1, P2, P3, P4, P5)
SEED_CHAT_IDS = (CH1, CH2, CH3, CH4)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clear and optionally repopulate selected HiBuddy database domains.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Examples:
  python backend/reset_database.py --reset messages --yes
  python backend/reset_database.py --reset matches --yes
  python backend/reset_database.py --reset projects --yes
  python backend/reset_database.py --reset all --keep users --yes
  python backend/reset_database.py --list-domains
""",
    )
    parser.add_argument(
        "--reset",
        nargs="+",
        choices=DOMAIN_CHOICES,
        help="Domain(s) to reset. Use 'all' to reset every supported domain.",
    )
    parser.add_argument(
        "--keep",
        nargs="*",
        choices=BASE_DOMAINS,
        default=[],
        help="Domain(s) to keep when --reset all, or to reject if implied by another reset.",
    )
    parser.add_argument(
        "--database-url",
        default=None,
        help="Override the async SQLAlchemy database URL. Defaults to app settings DATABASE_URL.",
    )
    parser.add_argument(
        "--no-seed",
        action="store_true",
        help="Only clear selected tables; do not repopulate seed rows.",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="Execute the reset. Without this flag the script only prints a plan.",
    )
    parser.add_argument(
        "--list-domains",
        action="store_true",
        help="Print reset domains and exit.",
    )
    args = parser.parse_args()

    if args.list_domains:
        return args

    if not args.reset:
        parser.error("--reset is required unless --list-domains is used")

    return args


def normalize_domains(raw_domains: Iterable[str]) -> set[str]:
    domains = set(raw_domains)
    if "all" in domains:
        return set(BASE_DOMAINS)
    return domains


def expand_domains(domains: Iterable[str]) -> set[str]:
    expanded = set(domains)
    changed = True
    while changed:
        changed = False
        for domain in tuple(expanded):
            implied = RESET_IMPLICATIONS.get(domain, set())
            new_domains = implied - expanded
            if new_domains:
                expanded.update(new_domains)
                changed = True
    return expanded


def table_name(model: Model) -> str:
    return str(model.__tablename__)


def ordered_domains(domains: Iterable[str]) -> list[str]:
    domain_set = set(domains)
    return [domain for domain in BASE_DOMAINS if domain in domain_set]


def tables_for_domains(domains: Iterable[str]) -> set[Model]:
    tables: set[Model] = set()
    for domain in domains:
        tables.update(TABLES_BY_DOMAIN[domain])
    return tables


def ordered_tables(tables: Iterable[Model]) -> list[Model]:
    table_set = set(tables)
    return [model for model in DELETE_ORDER if model in table_set]


def build_seed_steps(domains: set[str], no_seed: bool) -> list[SeedStep]:
    if no_seed:
        return []

    steps: list[SeedStep] = []
    if "users" in domains:
        steps.append(("users", _seed_users))
    if "projects" in domains:
        steps.append(("projects", _seed_projects))
        steps.append(("project members", _seed_members))
    if "tasks" in domains:
        steps.append(("tasks", _seed_tasks))
    if "swipes" in domains:
        steps.append(("swipes", _seed_swipes))
    if "matches" in domains:
        steps.append(("matches, chats, and messages", _seed_matches_and_chats))
    elif "messages" in domains:
        steps.append(("messages", _seed_messages))
    if "projects" in domains:
        steps.append(("project evaluations", _seed_evaluations))
    if "notifications" in domains:
        steps.append(("notifications", _seed_notifications))
    return steps


def mask_database_url(url: str) -> str:
    parsed = urlsplit(url)
    if parsed.password is None:
        return url

    username = parsed.username or ""
    host = parsed.hostname or ""
    if parsed.port is not None:
        host = f"{host}:{parsed.port}"
    auth = f"{username}:***@" if username else ""
    return urlunsplit((parsed.scheme, f"{auth}{host}", parsed.path, parsed.query, parsed.fragment))


def qualified_table_name(conn: Connection, table: Table) -> str:
    preparer = conn.dialect.identifier_preparer
    table_name = preparer.quote(table.name)
    if table.schema:
        return f"{preparer.quote_schema(table.schema)}.{table_name}"
    return table_name


def scalar_python_default(column: Column) -> object | None:
    default = column.default
    if default is None or not getattr(default, "is_scalar", False):
        return None
    return default.arg


def literal_default(conn: Connection, value: object) -> str:
    return str(
        literal(value).compile(
            dialect=conn.dialect,
            compile_kwargs={"literal_binds": True},
        )
    )


def column_definition(conn: Connection, column: Column, table: Table) -> tuple[str, str | None]:
    definition = str(CreateColumn(column).compile(dialect=conn.dialect))
    cleanup_sql = None

    if column.server_default is None and not column.nullable:
        default_value = scalar_python_default(column)
        if default_value is None:
            row_count = conn.execute(select(func.count()).select_from(table)).scalar_one()
            if row_count:
                raise RuntimeError(
                    f"Cannot add non-null column {table.name}.{column.name} without a default "
                    f"because {table.name} already has {row_count} row(s). Add a real migration "
                    "or clear the table before syncing schema."
                )
        else:
            default_sql = literal_default(conn, default_value)
            not_null = " NOT NULL"
            if definition.endswith(not_null):
                definition = f"{definition[:-len(not_null)]} DEFAULT {default_sql}{not_null}"
            else:
                definition = f"{definition} DEFAULT {default_sql}"
            cleanup_sql = (
                f"ALTER TABLE {qualified_table_name(conn, table)} "
                f"ALTER COLUMN {conn.dialect.identifier_preparer.quote(column.name)} DROP DEFAULT"
            )

    return definition, cleanup_sql


def sync_metadata_columns(conn: Connection) -> list[str]:
    """Apply small additive schema updates that create_all() skips.

    This keeps the reset script useful for local/dev databases after model-only
    changes while still refusing unsafe non-null additions that need a migration.
    """
    inspector = inspect(conn)
    updates: list[str] = []

    for table in Base.metadata.sorted_tables:
        if table.name not in inspector.get_table_names(schema=table.schema):
            continue

        existing_columns = {
            column["name"]
            for column in inspector.get_columns(table.name, schema=table.schema)
        }
        for column in table.columns:
            if column.name in existing_columns:
                continue

            definition, cleanup_sql = column_definition(conn, column, table)
            conn.execute(text(f"ALTER TABLE {qualified_table_name(conn, table)} ADD COLUMN {definition}"))
            if cleanup_sql:
                conn.execute(text(cleanup_sql))

            existing_columns.add(column.name)
            updates.append(f"added {table.name}.{column.name}")

    return updates


async def missing_ids(db: AsyncSession, model: Model, ids: Iterable[object]) -> list[str]:
    expected = set(ids)
    result = await db.execute(select(model.id).where(model.id.in_(expected)))
    found = set(result.scalars().all())
    return [str(item) for item in expected - found]


async def validate_seed_prerequisites(db: AsyncSession, domains: set[str]) -> None:
    errors: list[str] = []

    if "users" not in domains:
        needs_users = {"projects", "tasks", "swipes", "matches", "messages", "notifications"} & domains
        if needs_users:
            missing = await missing_ids(db, User, SEED_USER_IDS)
            if missing:
                errors.append(
                    "Seed users are missing but required for "
                    f"{', '.join(sorted(needs_users))}. Reset users too, or run a full seed first. "
                    f"Missing user ids: {', '.join(missing)}"
                )

    if "projects" not in domains:
        needs_projects = {"tasks", "swipes", "matches"} & domains
        if needs_projects:
            missing = await missing_ids(db, Project, SEED_PROJECT_IDS)
            if missing:
                errors.append(
                    "Seed projects are missing but required for "
                    f"{', '.join(sorted(needs_projects))}. Reset projects too, or run a full seed first. "
                    f"Missing project ids: {', '.join(missing)}"
                )

    if "matches" not in domains and "messages" in domains:
        missing = await missing_ids(db, Chat, SEED_CHAT_IDS)
        if missing:
            errors.append(
                "Seed chats are missing but required to reset only messages. "
                "Reset matches too, or run a full seed first. "
                f"Missing chat ids: {', '.join(missing)}"
            )

    if errors:
        raise RuntimeError("\n".join(errors))


async def count_tables(db: AsyncSession, tables: Iterable[Model]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for model in ordered_tables(tables):
        result = await db.execute(select(func.count()).select_from(model))
        counts[table_name(model)] = int(result.scalar_one())
    return counts


async def clear_tables(db: AsyncSession, tables: Iterable[Model]) -> dict[str, int | str]:
    deleted: dict[str, int | str] = {}
    for model in ordered_tables(tables):
        result = await db.execute(delete(model))
        deleted[table_name(model)] = result.rowcount if result.rowcount is not None else "unknown"
    await db.flush()
    return deleted


def print_domains() -> None:
    print("Reset domains:")
    for domain in BASE_DOMAINS:
        print(f"  {domain}: {DOMAIN_DESCRIPTIONS[domain]}")


def print_plan(
    database_url: str,
    requested: set[str],
    effective: set[str],
    kept: set[str],
    tables: list[Model],
    seed_steps: list[SeedStep],
    counts: dict[str, int],
    execute: bool,
    schema_updates: list[str] | None = None,
) -> None:
    print(f"Database: {mask_database_url(database_url)}")
    print(f"Requested reset domains: {', '.join(ordered_domains(requested)) or '(none)'}")
    if kept:
        print(f"Requested keep domains: {', '.join(ordered_domains(kept))}")
    print(f"Effective reset domains: {', '.join(ordered_domains(effective)) or '(none)'}")
    if schema_updates is not None:
        print("Schema updates:")
        if schema_updates:
            for update in schema_updates:
                print(f"  {update}")
        else:
            print("  none")
    print("Tables to clear:")
    for model in tables:
        name = table_name(model)
        print(f"  {name}: {counts.get(name, 0)} row(s)")

    if seed_steps:
        print("Seed steps:")
        for name, _ in seed_steps:
            print(f"  {name}")
    else:
        print("Seed steps: none")

    if not execute:
        print("Dry run only. Re-run with --yes to execute this reset.")


def resolve_domains(args: argparse.Namespace) -> tuple[set[str], set[str], set[str]]:
    requested = normalize_domains(args.reset)
    kept = set(args.keep)
    requested -= kept
    effective = expand_domains(requested)
    conflicts = effective & kept
    if conflicts:
        conflict_list = ", ".join(ordered_domains(conflicts))
        raise SystemExit(
            "Cannot keep the following domain(s) because another selected reset requires them: "
            f"{conflict_list}"
        )
    return requested, effective, kept


async def run_reset(args: argparse.Namespace) -> None:
    requested, effective, kept = resolve_domains(args)
    tables = ordered_tables(tables_for_domains(effective))
    seed_steps = build_seed_steps(effective, args.no_seed)
    settings = get_settings()
    database_url = args.database_url or settings.DATABASE_URL

    engine = create_async_engine(database_url, echo=False)
    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
    schema_updates: list[str] | None = None

    try:
        if args.yes:
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)
                schema_updates = await conn.run_sync(sync_metadata_columns)

        async with session_factory() as db:
            counts_before = await count_tables(db, tables)
            print_plan(
                database_url=database_url,
                requested=requested,
                effective=effective,
                kept=kept,
                tables=tables,
                seed_steps=seed_steps,
                counts=counts_before,
                execute=args.yes,
                schema_updates=schema_updates,
            )

            if not args.yes:
                return

            if seed_steps:
                await validate_seed_prerequisites(db, effective)

            deleted = await clear_tables(db, tables)
            for _, seed_step in seed_steps:
                await seed_step(db)
            await db.commit()

        async with session_factory() as db:
            counts_after = await count_tables(db, tables)

        print("Reset completed.")
        print("Deleted rows:")
        for name, count in deleted.items():
            print(f"  {name}: {count}")
        print("Final row counts:")
        for name, count in counts_after.items():
            print(f"  {name}: {count}")
    finally:
        await engine.dispose()


def main() -> None:
    args = parse_args()
    if args.list_domains:
        print_domains()
        return
    asyncio.run(run_reset(args))


if __name__ == "__main__":
    main()
