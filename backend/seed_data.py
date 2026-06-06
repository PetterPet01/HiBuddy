#!/usr/bin/env python3
"""Seed the HiBuddy database with comprehensive mock data for testing all features."""

import asyncio
import uuid
from datetime import datetime, timedelta, timezone
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy import select

from app.core.security import get_password_hash
from app.config import get_settings
from app.database import Base
from app.models.user import User
from app.models.profile import UserProfile, UserRole, UserSkill, UserInterest, UserCompletedCourse
from app.models.project import Project, ProjectRoleSlot, ProjectMember
from app.models.swipe import SwipeAction, Match
from app.models.task import Task, TaskCheckoutHistory, ProjectEvaluation
from app.models.chat import Chat, Message, Notification
from app.models.catalog import (
    ProjectRoleSkillRequirement,
    RoleCatalog,
    RoleSkillCatalog,
    SkillCatalog,
    UserRoleSkill,
)
from app.models.trust_safety import Report, UserBlock

settings = get_settings()

SYNC_ENGINE_URL = settings.DATABASE_URL_SYNC
ASYNC_ENGINE_URL = settings.DATABASE_URL


def now():
    return datetime.now(timezone.utc)


def days_from_now(n: int) -> datetime:
    return now() + timedelta(days=n)


def days_ago(n: int) -> datetime:
    return now() - timedelta(days=n)


DEMO_PASSWORD = "HiBuddyDemo!2026"
PASSWORD = get_password_hash(DEMO_PASSWORD)

# ── Pre-generated stable UUIDs for easy reference ──────────────────────────

U1  = uuid.UUID("10000000-0000-0000-0000-000000000001")
U2  = uuid.UUID("10000000-0000-0000-0000-000000000002")
U3  = uuid.UUID("10000000-0000-0000-0000-000000000003")
U4  = uuid.UUID("10000000-0000-0000-0000-000000000004")
U5  = uuid.UUID("10000000-0000-0000-0000-000000000005")
U6  = uuid.UUID("10000000-0000-0000-0000-000000000006")
U7  = uuid.UUID("10000000-0000-0000-0000-000000000007")
U8  = uuid.UUID("10000000-0000-0000-0000-000000000008")
U9  = uuid.UUID("10000000-0000-0000-0000-000000000009")
U10 = uuid.UUID("10000000-0000-0000-0000-000000000010")
U11 = uuid.UUID("10000000-0000-0000-0000-000000000011")

P1  = uuid.UUID("20000000-0000-0000-0000-000000000001")
P2  = uuid.UUID("20000000-0000-0000-0000-000000000002")
P3  = uuid.UUID("20000000-0000-0000-0000-000000000003")
P4  = uuid.UUID("20000000-0000-0000-0000-000000000004")
P5  = uuid.UUID("20000000-0000-0000-0000-000000000005")
P6  = uuid.UUID("20000000-0000-0000-0000-000000000006")

SLOT1 = uuid.UUID("30000000-0000-0000-0000-000000000001")
SLOT2 = uuid.UUID("30000000-0000-0000-0000-000000000002")
SLOT3 = uuid.UUID("30000000-0000-0000-0000-000000000003")
SLOT4 = uuid.UUID("30000000-0000-0000-0000-000000000004")
SLOT5 = uuid.UUID("30000000-0000-0000-0000-000000000005")
SLOT6 = uuid.UUID("30000000-0000-0000-0000-000000000006")
SLOT7 = uuid.UUID("30000000-0000-0000-0000-000000000007")
SLOT8 = uuid.UUID("30000000-0000-0000-0000-000000000008")
SLOT9 = uuid.UUID("30000000-0000-0000-0000-000000000009")
SLOT10 = uuid.UUID("30000000-0000-0000-0000-000000000010")

MEM1  = uuid.UUID("40000000-0000-0000-0000-000000000001")
MEM2  = uuid.UUID("40000000-0000-0000-0000-000000000002")
MEM3  = uuid.UUID("40000000-0000-0000-0000-000000000003")
MEM4  = uuid.UUID("40000000-0000-0000-0000-000000000004")
MEM5  = uuid.UUID("40000000-0000-0000-0000-000000000005")
MEM6  = uuid.UUID("40000000-0000-0000-0000-000000000006")
MEM7  = uuid.UUID("40000000-0000-0000-0000-000000000007")
MEM8  = uuid.UUID("40000000-0000-0000-0000-000000000008")
MEM9  = uuid.UUID("40000000-0000-0000-0000-000000000009")
MEM10 = uuid.UUID("40000000-0000-0000-0000-000000000010")
MEM11 = uuid.UUID("40000000-0000-0000-0000-000000000011")

T1  = uuid.UUID("50000000-0000-0000-0000-000000000001")
T2  = uuid.UUID("50000000-0000-0000-0000-000000000002")
T3  = uuid.UUID("50000000-0000-0000-0000-000000000003")
T4  = uuid.UUID("50000000-0000-0000-0000-000000000004")
T5  = uuid.UUID("50000000-0000-0000-0000-000000000005")
T6  = uuid.UUID("50000000-0000-0000-0000-000000000006")
T7  = uuid.UUID("50000000-0000-0000-0000-000000000007")
T8  = uuid.UUID("50000000-0000-0000-0000-000000000008")
T9  = uuid.UUID("50000000-0000-0000-0000-000000000009")
T10 = uuid.UUID("50000000-0000-0000-0000-000000000010")
T11 = uuid.UUID("50000000-0000-0000-0000-000000000011")
T12 = uuid.UUID("50000000-0000-0000-0000-000000000012")

H1 = uuid.UUID("60000000-0000-0000-0000-000000000001")
H2 = uuid.UUID("60000000-0000-0000-0000-000000000002")
H3 = uuid.UUID("60000000-0000-0000-0000-000000000003")

EV1 = uuid.UUID("70000000-0000-0000-0000-000000000001")
EV2 = uuid.UUID("70000000-0000-0000-0000-000000000002")

SW1  = uuid.UUID("80000000-0000-0000-0000-000000000001")
SW2  = uuid.UUID("80000000-0000-0000-0000-000000000002")
SW3  = uuid.UUID("80000000-0000-0000-0000-000000000003")
SW4  = uuid.UUID("80000000-0000-0000-0000-000000000004")
SW5  = uuid.UUID("80000000-0000-0000-0000-000000000005")
SW6  = uuid.UUID("80000000-0000-0000-0000-000000000006")
SW7  = uuid.UUID("80000000-0000-0000-0000-000000000007")
SW8  = uuid.UUID("80000000-0000-0000-0000-000000000008")
SW9  = uuid.UUID("80000000-0000-0000-0000-000000000009")
SW10 = uuid.UUID("80000000-0000-0000-0000-000000000010")
SW11 = uuid.UUID("80000000-0000-0000-0000-000000000011")
SW12 = uuid.UUID("80000000-0000-0000-0000-000000000012")
SW13 = uuid.UUID("80000000-0000-0000-0000-000000000013")
SW14 = uuid.UUID("80000000-0000-0000-0000-000000000014")
SW15 = uuid.UUID("80000000-0000-0000-0000-000000000015")
SW16 = uuid.UUID("80000000-0000-0000-0000-000000000016")
SW17 = uuid.UUID("80000000-0000-0000-0000-000000000017")
SW18 = uuid.UUID("80000000-0000-0000-0000-000000000018")

MA1 = uuid.UUID("90000000-0000-0000-0000-000000000001")
MA2 = uuid.UUID("90000000-0000-0000-0000-000000000002")
MA3 = uuid.UUID("90000000-0000-0000-0000-000000000003")
MA4 = uuid.UUID("90000000-0000-0000-0000-000000000004")

CH1 = uuid.UUID("a0000000-0000-0000-0000-000000000001")
CH2 = uuid.UUID("a0000000-0000-0000-0000-000000000002")
CH3 = uuid.UUID("a0000000-0000-0000-0000-000000000003")
CH4 = uuid.UUID("a0000000-0000-0000-0000-000000000004")

MSG1  = uuid.UUID("b0000000-0000-0000-0000-000000000001")
MSG2  = uuid.UUID("b0000000-0000-0000-0000-000000000002")
MSG3  = uuid.UUID("b0000000-0000-0000-0000-000000000003")
MSG4  = uuid.UUID("b0000000-0000-0000-0000-000000000004")
MSG5  = uuid.UUID("b0000000-0000-0000-0000-000000000005")
MSG6  = uuid.UUID("b0000000-0000-0000-0000-000000000006")
MSG7  = uuid.UUID("b0000000-0000-0000-0000-000000000007")
MSG8  = uuid.UUID("b0000000-0000-0000-0000-000000000008")
MSG9  = uuid.UUID("b0000000-0000-0000-0000-000000000009")
MSG10 = uuid.UUID("b0000000-0000-0000-0000-000000000010")

N1  = uuid.UUID("c0000000-0000-0000-0000-000000000001")
N2  = uuid.UUID("c0000000-0000-0000-0000-000000000002")
N3  = uuid.UUID("c0000000-0000-0000-0000-000000000003")
N4  = uuid.UUID("c0000000-0000-0000-0000-000000000004")
N5  = uuid.UUID("c0000000-0000-0000-0000-000000000005")
N6  = uuid.UUID("c0000000-0000-0000-0000-000000000006")
N7  = uuid.UUID("c0000000-0000-0000-0000-000000000007")

PROF1  = uuid.UUID("d0000000-0000-0000-0000-000000000001")
PROF2  = uuid.UUID("d0000000-0000-0000-0000-000000000002")
PROF3  = uuid.UUID("d0000000-0000-0000-0000-000000000003")
PROF4  = uuid.UUID("d0000000-0000-0000-0000-000000000004")
PROF5  = uuid.UUID("d0000000-0000-0000-0000-000000000005")
PROF6  = uuid.UUID("d0000000-0000-0000-0000-000000000006")
PROF7  = uuid.UUID("d0000000-0000-0000-0000-000000000007")
PROF8  = uuid.UUID("d0000000-0000-0000-0000-000000000008")
PROF10 = uuid.UUID("d0000000-0000-0000-0000-000000000010")
PROF11 = uuid.UUID("d0000000-0000-0000-0000-000000000011")


async def seed(engine):
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    async with async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)() as db:
        await _seed_users(db)
        await _seed_projects(db)
        await _seed_members(db)
        await _seed_tasks(db)
        await _seed_swipes(db)
        await _seed_matches_and_chats(db)
        await _seed_evaluations(db)
        await _seed_notifications(db)
        await _seed_trust_safety(db)
        await db.commit()
        print("Seed completed successfully.")
        print(f"   {_user_count()} users, {_project_count()} projects, {_task_count()} tasks, "
              f"{_swipe_count()} swipes, {_match_count()} matches, {_msg_count()} messages, "
              f"{_notif_count()} notifications")
        print(f"Demo password for all seeded accounts: {DEMO_PASSWORD}")


# ═══════════════════════════════════════════════════════════════════════════
# Users
# ═══════════════════════════════════════════════════════════════════════════

_user_count = lambda: 11
_project_count = lambda: 6
_task_count = lambda: 12
_swipe_count = lambda: 18
_match_count = lambda: 4
_msg_count = lambda: 10
_notif_count = lambda: 7


async def _seed_users(db: AsyncSession):
    users = [
        User(id=U1, username="minh_nguyen", email="minh@example.com", full_name="Minh Nguyen",
             hashed_password=PASSWORD, phone="+84912345678", date_of_birth=datetime(2001, 5, 15, tzinfo=timezone.utc),
             email_verified=True, verified_student=True, student_email="minh@hust.edu.vn",
             university="Hanoi University of Science and Technology", student_id="20191234",
             verification_status="APPROVED", role="MEMBER", created_at=days_ago(60), last_login=days_ago(1)),

        User(id=U2, username="thu_le", email="thu@example.com", full_name="Thu Le",
             hashed_password=PASSWORD, phone="+84987654321", date_of_birth=datetime(2002, 8, 22, tzinfo=timezone.utc),
             email_verified=True, verified_student=True, student_email="thu@vnu.edu.vn",
             university="Vietnam National University", student_id="20204567",
             verification_status="APPROVED", role="MEMBER", created_at=days_ago(55), last_login=days_ago(2)),

        User(id=U3, username="khoa_tran", email="khoa@example.com", full_name="Khoa Tran",
             hashed_password=PASSWORD, phone="+84911234567", date_of_birth=datetime(2000, 3, 10, tzinfo=timezone.utc),
             email_verified=True, verified_student=True, student_email="khoa@hcmut.edu.vn",
             university="Ho Chi Minh University of Technology", student_id="20181122",
             verification_status="APPROVED", role="MEMBER", created_at=days_ago(50), last_login=days_ago(1)),

        User(id=U4, username="linh_pham", email="linh@example.com", full_name="Linh Pham",
             hashed_password=PASSWORD, phone="+84933445566", date_of_birth=datetime(2001, 11, 30, tzinfo=timezone.utc),
             email_verified=True, verified_student=False, role="MEMBER",
             created_at=days_ago(40), last_login=days_ago(5)),

        User(id=U5, username="hai_vo", email="hai@example.com", full_name="Hai Vo",
             hashed_password=PASSWORD, phone="+84955667788", date_of_birth=datetime(2003, 1, 5, tzinfo=timezone.utc),
             email_verified=True, verified_student=False, role="MEMBER",
             created_at=days_ago(30), last_login=days_ago(3)),

        User(id=U6, username="mai_nguyen", email="mai@example.com", full_name="Mai Nguyen",
             hashed_password=PASSWORD, phone="+84966778899", date_of_birth=datetime(2002, 7, 18, tzinfo=timezone.utc),
             email_verified=True, verified_student=True, student_email="mai@rmit.edu.vn",
             university="RMIT Vietnam", student_id="20203456",
             verification_status="APPROVED", role="MEMBER", created_at=days_ago(25), last_login=days_ago(0)),

        User(id=U7, username="huy_dang", email="huy@example.com", full_name="Huy Dang",
             hashed_password=PASSWORD, phone="+84977889911", date_of_birth=datetime(2000, 9, 12, tzinfo=timezone.utc),
             email_verified=True, verified_student=False, role="MEMBER",
             created_at=days_ago(20), last_login=days_ago(1)),

        User(id=U8, username="lan_tran", email="lan@example.com", full_name="Lan Tran",
             hashed_password=PASSWORD, phone="+84988991122", date_of_birth=datetime(2001, 4, 25, tzinfo=timezone.utc),
             email_verified=True, verified_student=False, student_email="lan@ftu.edu.vn",
             university="Foreign Trade University", student_id="20210123",
             academic_year="Year 4", student_card_image_url="https://placehold.co/1000x640?text=Student+Card",
             verification_status="PENDING", verification_submitted_at=days_ago(1),
             role="MEMBER", created_at=days_ago(15), last_login=days_ago(0)),

        User(id=U9, username="admin", email="admin@hibuddy.local", full_name="HiBuddy Admin",
             hashed_password=PASSWORD, date_of_birth=datetime(1995, 1, 1, tzinfo=timezone.utc),
             email_verified=True, verified_student=True, verification_status="APPROVED",
             role="ADMIN", created_at=days_ago(120), last_login=days_ago(0)),

        User(id=U10, username="pending_email", email="pending@example.com", full_name="Pending Email",
             hashed_password=PASSWORD, date_of_birth=datetime(2003, 6, 12, tzinfo=timezone.utc),
             email_verified=False, verified_student=False, verification_status="NOT_SUBMITTED",
             role="MEMBER", created_at=days_ago(2)),

        User(id=U11, username="banned_demo", email="banned@example.com", full_name="Banned Demo",
             hashed_password=PASSWORD, date_of_birth=datetime(2000, 2, 20, tzinfo=timezone.utc),
             email_verified=True, verified_student=False, verification_status="REJECTED",
             verification_rejection_reason="Seeded moderation scenario",
             role="MEMBER", is_active=False, created_at=days_ago(45)),
    ]
    db.add_all(users)
    await db.flush()

    profiles = [
        UserProfile(id=PROF1, user_id=U1, display_name="Minh Nguyen",
                    bio="Full-stack developer passionate about building products that make a difference. Love working on EdTech and AI projects.",
                    location="Hanoi, Vietnam", portfolio_url="https://minh.dev",
                    github_url="https://github.com/minhn", facebook_url=None,
                    short_term_goal="Build a production-ready SaaS product", mode="BOTH",
                    is_hidden=False, reputation_score=4.5, projects_completed=6,
                    created_at=days_ago(60)),

        UserProfile(id=PROF2, user_id=U2, display_name="Thu Le",
                    bio="UI/UX designer with a knack for user research. I believe great design solves real problems.",
                    location="Hanoi, Vietnam", portfolio_url="https://dribbble.com/thule",
                    github_url="https://github.com/thule", facebook_url="https://facebook.com/thule",
                    short_term_goal="Lead design for a mobile app", mode="CONTRIBUTOR",
                    is_hidden=False, reputation_score=4.8, projects_completed=4,
                    created_at=days_ago(55)),

        UserProfile(id=PROF3, user_id=U3, display_name="Khoa Tran",
                    bio="Backend engineer specializing in distributed systems and cloud architecture. AWS certified.",
                    location="Ho Chi Minh City, Vietnam", portfolio_url=None,
                    github_url="https://github.com/khoatran", facebook_url=None,
                    short_term_goal="Contribute to open source at scale", mode="BOTH",
                    is_hidden=False, reputation_score=4.2, projects_completed=8,
                    created_at=days_ago(50)),

        UserProfile(id=PROF4, user_id=U4, display_name="Linh Pham",
                    bio="Frontend developer and CSS magician. React, Flutter, you name it.",
                    location="Da Nang, Vietnam", portfolio_url="https://linhpham.dev",
                    github_url="https://github.com/linhp", facebook_url=None,
                    short_term_goal="Launch a developer portfolio template", mode="CONTRIBUTOR",
                    is_hidden=False, reputation_score=3.8, projects_completed=3,
                    created_at=days_ago(40)),

        UserProfile(id=PROF5, user_id=U5, display_name="Hai Vo",
                    bio="AI/ML enthusiast, currently exploring NLP and LLMs. Mathematics background.",
                    location="Hanoi, Vietnam", portfolio_url=None,
                    github_url="https://github.com/haivo", facebook_url=None,
                    short_term_goal="Publish a paper on Vietnamese NLP", mode="BOTH",
                    is_hidden=False, reputation_score=4.0, projects_completed=2,
                    created_at=days_ago(30)),

        UserProfile(id=PROF6, user_id=U6, display_name="Mai Nguyen",
                    bio="Product manager by day, coder by night. Love connecting tech with business value.",
                    location="Ho Chi Minh City, Vietnam", portfolio_url=None,
                    github_url="https://github.com/maing", facebook_url="https://facebook.com/mainguyen",
                    short_term_goal="Get PMP certified", mode="BOTH",
                    is_hidden=False, reputation_score=4.6, projects_completed=5,
                    created_at=days_ago(25)),

        UserProfile(id=PROF7, user_id=U7, display_name="Huy Dang",
                    bio="DevOps and infrastructure engineer. Terraform, K8s, CI/CD.",
                    location="Hanoi, Vietnam", portfolio_url=None,
                    github_url="https://github.com/huyd", facebook_url=None,
                    short_term_goal="Automate everything", mode="CONTRIBUTOR",
                    is_hidden=False, reputation_score=3.5, projects_completed=1,
                    created_at=days_ago(20)),

        UserProfile(id=PROF8, user_id=U8, display_name="Lan Tran",
                    bio="Marketing and growth enthusiast exploring how tech can scale businesses.",
                    location="Hanoi, Vietnam", portfolio_url="https://lantran.me",
                    github_url=None, facebook_url="https://facebook.com/lantran",
                    short_term_goal="Design a growth framework for startups", mode="BOTH",
                    is_hidden=False, reputation_score=3.2, projects_completed=0,
                    created_at=days_ago(15)),

        UserProfile(id=PROF10, user_id=U10, display_name="Pending Email",
                    bio="Profile used to verify that unverified accounts cannot access protected discovery flows.",
                    location="Hanoi, Vietnam", mode="CONTRIBUTOR", is_hidden=False,
                    reputation_score=3.0, projects_completed=0, created_at=days_ago(2)),

        UserProfile(id=PROF11, user_id=U11, display_name="Banned Demo",
                    bio="Profile used to verify session revocation, moderation, and discovery filtering.",
                    location="Da Nang, Vietnam", mode="CONTRIBUTOR", is_hidden=False,
                    reputation_score=2.0, projects_completed=0, created_at=days_ago(45)),
    ]
    db.add_all(profiles)
    await db.flush()

    roles = [
        UserRole(id=uuid.uuid4(), user_id=U1, role_name="Full-Stack Developer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U1, role_name="Technical Lead", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U2, role_name="UI/UX Designer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U2, role_name="Product Designer", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U2, role_name="Design Lead", ordering=2),
        UserRole(id=uuid.uuid4(), user_id=U3, role_name="Backend Developer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U3, role_name="Cloud Architect", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U4, role_name="Frontend Developer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U4, role_name="Mobile Developer", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U5, role_name="AI/ML Engineer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U5, role_name="Data Scientist", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U6, role_name="Product Manager", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U6, role_name="Business Analyst", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U7, role_name="DevOps Engineer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U8, role_name="Marketing Lead", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U8, role_name="Content Strategist", ordering=1),
        UserRole(id=uuid.uuid4(), user_id=U10, role_name="QA Engineer", ordering=0),
        UserRole(id=uuid.uuid4(), user_id=U11, role_name="Frontend Developer", ordering=0),
    ]
    db.add_all(roles)
    await db.flush()

    skills = [
        UserSkill(id=uuid.uuid4(), user_id=U1, skill_name="Python", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U1, skill_name="TypeScript", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U1, skill_name="React", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U1, skill_name="Docker", level="INTERMEDIATE"),
        UserSkill(id=uuid.uuid4(), user_id=U2, skill_name="Figma", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U2, skill_name="User Research", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U2, skill_name="Design Systems", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U3, skill_name="Go", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U3, skill_name="PostgreSQL", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U3, skill_name="AWS", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U3, skill_name="Kubernetes", level="INTERMEDIATE"),
        UserSkill(id=uuid.uuid4(), user_id=U4, skill_name="Flutter", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U4, skill_name="React Native", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U4, skill_name="CSS", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U5, skill_name="Python", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U5, skill_name="TensorFlow", level="INTERMEDIATE"),
        UserSkill(id=uuid.uuid4(), user_id=U5, skill_name="PyTorch", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U5, skill_name="NLP", level="BEGINNER", needs_improvement=True),
        UserSkill(id=uuid.uuid4(), user_id=U6, skill_name="Agile", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U6, skill_name="Jira", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U6, skill_name="SQL", level="INTERMEDIATE"),
        UserSkill(id=uuid.uuid4(), user_id=U7, skill_name="Terraform", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U7, skill_name="Kubernetes", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U7, skill_name="CI/CD", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U8, skill_name="SEO", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U8, skill_name="Content Marketing", level="ADVANCED"),
        UserSkill(id=uuid.uuid4(), user_id=U8, skill_name="Google Analytics", level="INTERMEDIATE"),
        UserSkill(id=uuid.uuid4(), user_id=U10, skill_name="Test Automation", level="BEGINNER"),
        UserSkill(id=uuid.uuid4(), user_id=U11, skill_name="React", level="INTERMEDIATE"),
    ]
    db.add_all(skills)
    await db.flush()
    await _seed_role_scoped_skills(db, roles, skills)

    interests = [
        UserInterest(id=uuid.uuid4(), user_id=U1, interest_name="AI/ML"),
        UserInterest(id=uuid.uuid4(), user_id=U1, interest_name="EdTech"),
        UserInterest(id=uuid.uuid4(), user_id=U1, interest_name="Open Source"),
        UserInterest(id=uuid.uuid4(), user_id=U2, interest_name="UX Design"),
        UserInterest(id=uuid.uuid4(), user_id=U2, interest_name="Accessibility"),
        UserInterest(id=uuid.uuid4(), user_id=U2, interest_name="HealthTech"),
        UserInterest(id=uuid.uuid4(), user_id=U3, interest_name="Cloud Computing"),
        UserInterest(id=uuid.uuid4(), user_id=U3, interest_name="FinTech"),
        UserInterest(id=uuid.uuid4(), user_id=U4, interest_name="Mobile Dev"),
        UserInterest(id=uuid.uuid4(), user_id=U4, interest_name="Game Dev"),
        UserInterest(id=uuid.uuid4(), user_id=U5, interest_name="AI/ML"),
        UserInterest(id=uuid.uuid4(), user_id=U5, interest_name="NLP"),
        UserInterest(id=uuid.uuid4(), user_id=U6, interest_name="Product Strategy"),
        UserInterest(id=uuid.uuid4(), user_id=U6, interest_name="Startups"),
        UserInterest(id=uuid.uuid4(), user_id=U7, interest_name="Infrastructure"),
        UserInterest(id=uuid.uuid4(), user_id=U7, interest_name="Automation"),
        UserInterest(id=uuid.uuid4(), user_id=U8, interest_name="Growth Hacking"),
        UserInterest(id=uuid.uuid4(), user_id=U8, interest_name="E-commerce"),
    ]
    db.add_all(interests)
    await db.flush()

    courses = [
        UserCompletedCourse(id=uuid.uuid4(), user_id=U1, course_title="CS50: Introduction to Computer Science",
                            source="edX", completed_date=days_ago(500), badge_visible=True, course_id="cs50"),
        UserCompletedCourse(id=uuid.uuid4(), user_id=U1, course_title="Machine Learning Specialization",
                            source="Coursera", completed_date=days_ago(200), badge_visible=True, course_id="ml-spec"),
        UserCompletedCourse(id=uuid.uuid4(), user_id=U2, course_title="Google UX Design Certificate",
                            source="Coursera", completed_date=days_ago(300), badge_visible=True, course_id="google-ux"),
        UserCompletedCourse(id=uuid.uuid4(), user_id=U3, course_title="AWS Solutions Architect",
                            source="AWS Training", completed_date=days_ago(150), badge_visible=True, course_id="aws-sa"),
        UserCompletedCourse(id=uuid.uuid4(), user_id=U4, course_title="Flutter & Dart - The Complete Guide",
                            source="Udemy", completed_date=days_ago(100), badge_visible=True, course_id="flutter-udemy"),
        UserCompletedCourse(id=uuid.uuid4(), user_id=U5, course_title="Deep Learning Specialization",
                            source="Coursera", completed_date=days_ago(180), badge_visible=True, course_id="deep-learning"),
    ]
    db.add_all(courses)
    await db.flush()


def _slug(value: str) -> str:
    return "-".join(value.lower().replace("/", " ").split())


async def _seed_role_scoped_skills(
    db: AsyncSession, roles: list[UserRole], skills: list[UserSkill]
) -> None:
    role_skill_hints = {
        "Full-Stack Developer": {"Python", "TypeScript", "React", "Docker"},
        "Technical Lead": {"Python", "Docker"},
        "UI/UX Designer": {"Figma", "User Research", "Design Systems"},
        "Product Designer": {"Figma", "User Research"},
        "Design Lead": {"Design Systems", "User Research"},
        "Backend Developer": {"Go", "PostgreSQL", "AWS"},
        "Cloud Architect": {"AWS", "Kubernetes", "PostgreSQL"},
        "Frontend Developer": {"React", "CSS"},
        "Mobile Developer": {"Flutter", "React Native"},
        "AI/ML Engineer": {"Python", "TensorFlow", "PyTorch", "NLP"},
        "Data Scientist": {"Python", "TensorFlow", "NLP"},
        "Product Manager": {"Agile", "Jira"},
        "Business Analyst": {"SQL", "Jira"},
        "DevOps Engineer": {"Terraform", "Kubernetes", "CI/CD"},
        "Marketing Lead": {"SEO", "Google Analytics"},
        "Content Strategist": {"Content Marketing", "SEO"},
        "QA Engineer": {"Test Automation"},
    }
    role_names = sorted({role.role_name for role in roles})
    skill_names = sorted({skill.skill_name for skill in skills})
    role_catalog = {
        name: RoleCatalog(id=uuid.uuid4(), slug=_slug(name), name=name)
        for name in role_names
    }
    skill_catalog = {
        name: SkillCatalog(id=uuid.uuid4(), slug=_slug(name), name=name)
        for name in skill_names
    }
    db.add_all([*role_catalog.values(), *skill_catalog.values()])
    await db.flush()

    skills_by_user: dict[uuid.UUID, list[UserSkill]] = {}
    for skill in skills:
        skills_by_user.setdefault(skill.user_id, []).append(skill)

    catalog_pairs: set[tuple[uuid.UUID, uuid.UUID]] = set()
    scoped_skills: list[UserRoleSkill] = []
    catalog_links: list[RoleSkillCatalog] = []
    for role in roles:
        role.catalog_role_id = role_catalog[role.role_name].id
        available_skills = skills_by_user.get(role.user_id, [])
        hinted_names = role_skill_hints.get(role.role_name)
        user_skills = (
            [skill for skill in available_skills if skill.skill_name in hinted_names]
            if hinted_names
            else available_skills
        )
        for skill in user_skills:
            skill_row = skill_catalog[skill.skill_name]
            scoped_skills.append(
                UserRoleSkill(
                    user_role_id=role.id,
                    skill_id=skill_row.id,
                    level=skill.level,
                    needs_improvement=skill.needs_improvement,
                )
            )
            pair = (role.catalog_role_id, skill_row.id)
            if pair not in catalog_pairs:
                catalog_pairs.add(pair)
                catalog_links.append(
                    RoleSkillCatalog(role_id=pair[0], skill_id=pair[1])
                )
    db.add_all([*scoped_skills, *catalog_links])
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Projects
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_projects(db: AsyncSession):
    projects = [
        Project(
            id=P1, owner_id=U1,
            title="StudyMate - AI Study Planner", field="EdTech",
            description="An AI-powered study planner that creates personalized learning schedules based on student goals, available time, and learning style. Integrates with Google Calendar and university LMS systems.",
            specific_goal="Launch MVP with 1000 beta users within 3 months",
            work_mode="ONLINE", commitment_level="INTENSIVE",
            start_date=days_from_now(7), end_date=days_from_now(97),
            max_members=6, status="RECRUITING",
            additional_requirements="Experience with LLM APIs (OpenAI, Anthropic) preferred",
            member_benefits="Equity share, mentorship from senior engineers, portfolio piece",
            created_at=days_ago(14),
        ),
        Project(
            id=P2, owner_id=U3,
            title="GreenTrack - Carbon Footprint App", field="Climate Tech",
            description="Mobile app that tracks personal carbon footprint through daily activities, transportation, and consumption patterns. Gamifies reduction with challenges and community leaderboards.",
            specific_goal="Get featured on App Store / Google Play and reach 10k downloads",
            work_mode="HYBRID", commitment_level="CASUAL",
            start_date=days_from_now(3), end_date=days_from_now(63),
            max_members=4, status="RECRUITING",
            additional_requirements="Interest in climate change and sustainability",
            member_benefits="Learn React Native, contribute to a meaningful cause, networking opportunities",
            created_at=days_ago(10),
        ),
        Project(
            id=P3, owner_id=U6,
            title="LocalBites - Restaurant Discovery", field="Mobile",
            description="A hyperlocal restaurant discovery app that uses AI to recommend dishes based on your taste profile, not just reviews. Think Spotify but for food.",
            specific_goal="Pilot in District 1, Ho Chi Minh City with 50+ restaurant partners",
            work_mode="OFFLINE", commitment_level="INTENSIVE",
            start_date=days_from_now(14), end_date=days_from_now(104),
            max_members=5, status="RECRUITING",
            additional_requirements="Restaurant/food industry connections a plus",
            member_benefits="Revenue share, free meals during field research, great team",
            created_at=days_ago(21),
        ),
        Project(
            id=P4, owner_id=U1,
            title="DevPort - Developer Portfolio Platform", field="Web",
            description="A platform where developers can build stunning portfolios from their GitHub profile automatically. Includes analytics, blog integration, and customizable themes.",
            specific_goal=None,
            work_mode="ONLINE", commitment_level="CASUAL",
            start_date=days_ago(30), end_date=days_from_now(60),
            max_members=4, status="ACTIVE",
            additional_requirements=None,
            member_benefits="Open source contribution credit, mentorship",
            created_at=days_ago(35),
        ),
        Project(
            id=P5, owner_id=U5,
            title="VNLingua - Vietnamese NLP Toolkit", field="AI/ML",
            description="Open-source toolkit for Vietnamese natural language processing including tokenization, NER, sentiment analysis, and text summarization optimized for the Vietnamese language.",
            specific_goal="Release v1.0 with state-of-the-art accuracy for Vietnamese NER",
            work_mode="ONLINE", commitment_level="INTENSIVE",
            start_date=days_ago(60), end_date=days_ago(5),
            max_members=3, status="CLOSED",
            additional_requirements="Understanding of linguistics or NLP fundamentals",
            member_benefits="Research co-authorship, open source recognition",
            created_at=days_ago(65),
        ),
        Project(
            id=P6, owner_id=U1,
            title="Campus Marketplace Moderation Scenario", field="Marketplace",
            description="A seeded project held for manual review so administrators can test moderation evidence, approve and reject actions, required reasons, and audit logging.",
            specific_goal="Validate the complete project moderation workflow",
            work_mode="ONLINE", commitment_level="MODERATE",
            start_date=days_from_now(10), end_date=days_from_now(70),
            max_members=3, status="RECRUITING", review_status="MANUAL_REVIEW",
            moderation_categories=["marketplace_risk"],
            moderation_reasons=["Seeded manual-review scenario"],
            moderation_checked_at=days_ago(1),
            additional_requirements="Administrator review required before discovery",
            member_benefits="Moderation workflow testing",
            created_at=days_ago(1),
        ),
    ]
    db.add_all(projects)
    await db.flush()

    slots = [
        ProjectRoleSlot(id=SLOT1, project_id=P1, role_name="Flutter Developer", count=2, filled=0,
                        skill_requirements={"requirements": "Flutter, Dart, State Management"}),
        ProjectRoleSlot(id=SLOT2, project_id=P1, role_name="Backend Developer", count=1, filled=0,
                        skill_requirements={"requirements": "Python, FastAPI, PostgreSQL"}),
        ProjectRoleSlot(id=SLOT3, project_id=P1, role_name="UI/UX Designer", count=1, filled=0,
                        skill_requirements={"requirements": "Figma, User Research, Design Systems"}),
        ProjectRoleSlot(id=SLOT4, project_id=P2, role_name="React Native Developer", count=2, filled=0,
                        skill_requirements={"requirements": "React Native, TypeScript, Firebase"}),
        ProjectRoleSlot(id=SLOT5, project_id=P2, role_name="UI/UX Designer", count=1, filled=0,
                        skill_requirements=None),
        ProjectRoleSlot(id=SLOT6, project_id=P3, role_name="Android Developer", count=2, filled=0,
                        skill_requirements={"requirements": "Kotlin, Jetpack Compose, Retrofit"}),
        ProjectRoleSlot(id=SLOT7, project_id=P3, role_name="Backend Developer", count=1, filled=0,
                        skill_requirements={"requirements": "Node.js or Go, MongoDB, Redis"}),
        ProjectRoleSlot(id=SLOT8, project_id=P4, role_name="Frontend Developer", count=2, filled=0,
                        skill_requirements={"requirements": "React, Next.js, Tailwind"}),
        ProjectRoleSlot(id=SLOT9, project_id=P5, role_name="Python Developer", count=2, filled=0,
                        skill_requirements={"requirements": "Python, PyTorch, HuggingFace"}),
        ProjectRoleSlot(id=SLOT10, project_id=P6, role_name="Android Developer", count=2, filled=0,
                        skill_requirements={"Kotlin": "INTERMEDIATE", "Jetpack Compose": "INTERMEDIATE"}),
    ]
    db.add_all(slots)
    await db.flush()
    await _seed_project_skill_requirements(db, slots)


async def _seed_project_skill_requirements(
    db: AsyncSession, slots: list[ProjectRoleSlot]
) -> None:
    requirements = {
        SLOT1: [("Flutter", "INTERMEDIATE"), ("Dart", "INTERMEDIATE")],
        SLOT2: [("Python", "INTERMEDIATE"), ("FastAPI", "BEGINNER"), ("PostgreSQL", "INTERMEDIATE")],
        SLOT3: [("Figma", "INTERMEDIATE"), ("User Research", "INTERMEDIATE")],
        SLOT4: [("React Native", "INTERMEDIATE"), ("TypeScript", "INTERMEDIATE")],
        SLOT5: [("Figma", "INTERMEDIATE")],
        SLOT6: [("Kotlin", "INTERMEDIATE"), ("Jetpack Compose", "INTERMEDIATE")],
        SLOT7: [("Go", "INTERMEDIATE"), ("Redis", "BEGINNER")],
        SLOT8: [("React", "INTERMEDIATE"), ("TypeScript", "INTERMEDIATE")],
        SLOT9: [("Python", "ADVANCED"), ("PyTorch", "INTERMEDIATE")],
        SLOT10: [("Kotlin", "INTERMEDIATE"), ("Jetpack Compose", "INTERMEDIATE")],
    }
    existing = {
        row.name: row
        for row in (await db.execute(select(SkillCatalog))).scalars().all()
    }
    for skill_name in sorted({name for rows in requirements.values() for name, _ in rows}):
        if skill_name not in existing:
            row = SkillCatalog(slug=_slug(skill_name), name=skill_name)
            db.add(row)
            await db.flush()
            existing[skill_name] = row
    for slot in slots:
        rows = requirements.get(slot.id, [])
        if rows:
            slot.skill_requirements = {name: level for name, level in rows}
        for skill_name, minimum_level in rows:
            db.add(
                ProjectRoleSkillRequirement(
                    role_slot_id=slot.id,
                    skill_id=existing[skill_name].id,
                    minimum_level=minimum_level,
                    is_required=True,
                )
            )
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Members
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_members(db: AsyncSession):
    members = [
        ProjectMember(id=MEM1,  project_id=P1, user_id=U1, role="Project Owner", is_owner=True, joined_at=days_ago(14)),
        ProjectMember(id=MEM2,  project_id=P2, user_id=U3, role="Project Owner", is_owner=True, joined_at=days_ago(10)),
        ProjectMember(id=MEM3,  project_id=P3, user_id=U6, role="Project Owner", is_owner=True, joined_at=days_ago(21)),
        ProjectMember(id=MEM4,  project_id=P4, user_id=U1, role="Project Owner", is_owner=True, joined_at=days_ago(35)),
        ProjectMember(id=MEM5,  project_id=P5, user_id=U5, role="Project Owner", is_owner=True, joined_at=days_ago(65)),

        ProjectMember(id=MEM6,  project_id=P4, user_id=U2, role="UI/UX Designer", is_owner=False, joined_at=days_ago(30)),
        ProjectMember(id=MEM7,  project_id=P4, user_id=U4, role="Frontend Developer", is_owner=False, joined_at=days_ago(28)),
        ProjectMember(id=MEM8,  project_id=P5, user_id=U7, role="Python Developer", is_owner=False, joined_at=days_ago(55)),
        ProjectMember(id=MEM9,  project_id=P5, user_id=U8, role="Python Developer", is_owner=False, joined_at=days_ago(50)),
        ProjectMember(id=MEM10, project_id=P3, user_id=U2, role="UI/UX Designer", is_owner=False, joined_at=days_ago(18)),
        ProjectMember(id=MEM11, project_id=P6, user_id=U1, role="Project Owner", is_owner=True, joined_at=days_ago(1)),
    ]
    db.add_all(members)
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Tasks
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_tasks(db: AsyncSession):
    tasks = [
        Task(id=T1, project_id=P4, assignee_id=U2, creator_id=U1,
             title="Design system component library", description="Create a comprehensive Figma component library with auto-layout, variants, and documentation for all platform UI elements.",
             role_related="UI/UX Designer", priority="HIGH", status="CLOSED",
             start_date=days_ago(30), deadline=days_ago(10),
             tag="Design", checkout_at=days_ago(12), checkout_confirmed_at=days_ago(10),
             checkout_status="EARLY", reminder_sent=False),

        Task(id=T2, project_id=P4, assignee_id=U4, creator_id=U1,
             title="GitHub OAuth integration", description="Implement OAuth2 login with GitHub to automatically pull repository data for portfolio generation.",
             role_related="Frontend Developer", priority="HIGH", status="CLOSED",
             start_date=days_ago(28), deadline=days_ago(5),
             tag="Backend", checkout_at=days_ago(6), checkout_confirmed_at=days_ago(5),
             checkout_status="ON_TIME", reminder_sent=False),

        Task(id=T3, project_id=P4, assignee_id=U2, creator_id=U1,
             title="Landing page redesign", description="Redesign the marketing landing page with better CTAs, hero section, and pricing table.",
             role_related="UI/UX Designer", priority="MEDIUM", status="DONE_REVIEW",
             start_date=days_ago(20), deadline=days_from_now(2),
             tag="Design", checkout_at=days_ago(1), checkout_confirmed_at=None,
             checkout_status="EARLY", reminder_sent=False),

        Task(id=T4, project_id=P4, assignee_id=U4, creator_id=U1,
             title="Analytics dashboard", description="Build an analytics dashboard showing portfolio views, project clicks, and visitor demographics.",
             role_related="Frontend Developer", priority="MEDIUM", status="IN_PROGRESS",
             start_date=days_ago(15), deadline=days_from_now(5),
             tag="Feature", checkout_at=None, checkout_confirmed_at=None,
             checkout_status=None, reminder_sent=False),

        Task(id=T5, project_id=P4, assignee_id=U4, creator_id=U1,
             title="SEO optimization", description="Implement meta tags, open graph, structured data, and sitemap generation for all portfolio pages.",
             role_related="Frontend Developer", priority="LOW", status="TODO",
             start_date=days_from_now(1), deadline=days_from_now(14),
             tag="SEO", checkout_at=None, checkout_confirmed_at=None,
             checkout_status=None, reminder_sent=False),

        Task(id=T6, project_id=P4, assignee_id=U2, creator_id=U1,
             title="User onboarding flow", description="Design a 3-step onboarding wizard for first-time users connecting their GitHub account.",
             role_related="UI/UX Designer", priority="HIGH", status="TODO",
             start_date=days_from_now(3), deadline=days_from_now(10),
             tag="Design", checkout_at=None, checkout_confirmed_at=None,
             checkout_status=None, reminder_sent=True),

        Task(id=T7, project_id=P5, assignee_id=U7, creator_id=U5,
             title="Vietnamese tokenizer v2", description="Improve the word segmentation tokenizer to handle compound words and slang better. Target >95% accuracy on VNTQ dataset.",
             role_related="Python Developer", priority="HIGH", status="CLOSED",
             start_date=days_ago(50), deadline=days_ago(20),
             tag="NLP", checkout_at=days_ago(22), checkout_confirmed_at=days_ago(20),
             checkout_status="EARLY", reminder_sent=False),

        Task(id=T8, project_id=P5, assignee_id=U8, creator_id=U5,
             title="Sentiment analysis dataset", description="Curate and label a dataset of 10,000 Vietnamese sentences for sentiment analysis training.",
             role_related="Python Developer", priority="HIGH", status="CLOSED",
             start_date=days_ago(45), deadline=days_ago(15),
             tag="Data", checkout_at=days_ago(17), checkout_confirmed_at=days_ago(15),
             checkout_status="LATE", reminder_sent=False),

        Task(id=T9, project_id=P5, assignee_id=U7, creator_id=U5,
             title="Named Entity Recognition model", description="Fine-tune a transformer model for Vietnamese NER with 7 entity types (PERSON, ORG, LOC, DATE, EVENT, PRODUCT, MISC).",
             role_related="Python Developer", priority="MEDIUM", status="CLOSED",
             start_date=days_ago(40), deadline=days_ago(10),
             tag="ML", checkout_at=days_ago(12), checkout_confirmed_at=days_ago(10),
             checkout_status="ON_TIME", reminder_sent=False),

        Task(id=T10, project_id=P5, assignee_id=U8, creator_id=U5,
             title="API documentation", description="Write comprehensive API docs with usage examples for all NLP functions.",
             role_related="Python Developer", priority="LOW", status="CLOSED",
             start_date=days_ago(30), deadline=days_ago(5),
             tag="Docs", checkout_at=days_ago(3), checkout_confirmed_at=days_ago(2),
             checkout_status="LATE", reminder_sent=False),

        Task(id=T11, project_id=P3, assignee_id=U2, creator_id=U6,
             title="Restaurant partner onboarding flow", description="Design the UI for restaurant owners to register, upload menu, and manage their business profile.",
             role_related="UI/UX Designer", priority="HIGH", status="TODO",
             start_date=days_ago(15), deadline=days_from_now(10),
             tag="Design", checkout_at=None, checkout_confirmed_at=None,
             checkout_status=None, reminder_sent=False),

        Task(id=T12, project_id=P3, assignee_id=U2, creator_id=U6,
             title="Wireframe the recommendation engine UI", description="Create wireframes showing how personalized dish recommendations appear to users based on their taste profile.",
             role_related="UI/UX Designer", priority="MEDIUM", status="IN_PROGRESS",
             start_date=days_ago(10), deadline=days_from_now(7),
             tag="Design", checkout_at=None, checkout_confirmed_at=None,
             checkout_status=None, reminder_sent=False),
    ]
    db.add_all(tasks)
    await db.flush()

    histories = [
        TaskCheckoutHistory(id=H1, task_id=T1, action="CHECKOUT", actor_id=U2, previous_status="IN_PROGRESS",
                            new_status="DONE_REVIEW", timestamp=days_ago(12), notes="Checkout status: EARLY"),
        TaskCheckoutHistory(id=uuid.uuid4(), task_id=T1, action="CONFIRM", actor_id=U1,
                            previous_status="DONE_REVIEW", new_status="CLOSED",
                            timestamp=days_ago(10), notes="Owner confirmed checkout"),
        TaskCheckoutHistory(id=uuid.uuid4(), task_id=T2, action="CHECKOUT", actor_id=U4,
                            previous_status="IN_PROGRESS", new_status="DONE_REVIEW",
                            timestamp=days_ago(6), notes="Checkout status: ON_TIME"),
        TaskCheckoutHistory(id=uuid.uuid4(), task_id=T2, action="CONFIRM", actor_id=U1,
                            previous_status="DONE_REVIEW", new_status="CLOSED",
                            timestamp=days_ago(5), notes="Owner confirmed checkout"),
        TaskCheckoutHistory(id=uuid.uuid4(), task_id=T8, action="CHECKOUT", actor_id=U8,
                            previous_status="IN_PROGRESS", new_status="DONE_REVIEW",
                            timestamp=days_ago(17), notes="Checkout status: LATE"),
        TaskCheckoutHistory(id=H3, task_id=T8, action="OVERRIDE", actor_id=U5,
                            previous_status="LATE", new_status="LATE",
                            timestamp=days_ago(15), notes="Accepted with delay due to dataset complexity"),
    ]
    db.add_all(histories)
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Swipes
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_swipes(db: AsyncSession):
    """Create swipe actions that produce meaningful test scenarios.

    Match logic:
      - Contributor likes a project AND project owner liked the contributor → match
      - Owner likes a contributor AND contributor liked the owner's project → match
    """
    swipes = [
        # ── P1 (StudyMate, owner U1) ──
        # U2 swiped RIGHT on P1 → contributor likes project
        SwipeAction(id=SW1, swiper_id=U2, target_type="PROJECT", target_id=str(P1),
                    action="LIKE", created_at=days_ago(5), is_active=True),
        # U1 swiped RIGHT on U2 → owner liked contributor → MATCH Ma1 between U2 & P1 (owner U1)
        SwipeAction(id=SW2, swiper_id=U1, target_type="USER", target_id=str(U2),
                    action="LIKE", context_project_id=P1, context_role_slot_id=SLOT3,
                    context_key=str(P1), created_at=days_ago(4), is_active=True),

        # U4 swiped RIGHT on P1 (waiting for owner to like back)
        SwipeAction(id=SW3, swiper_id=U4, target_type="PROJECT", target_id=str(P1),
                    action="LIKE", created_at=days_ago(3), is_active=True),

        # U5 PASSED on P1
        SwipeAction(id=SW4, swiper_id=U5, target_type="PROJECT", target_id=str(P1),
                    action="PASS", created_at=days_ago(2), is_active=True),

        # ── P2 (GreenTrack, owner U3) ──
        # U1 swiped RIGHT on P2 → contributor likes project
        SwipeAction(id=SW5, swiper_id=U1, target_type="PROJECT", target_id=str(P2),
                    action="LIKE", created_at=days_ago(6), is_active=True),
        # U3 swiped RIGHT on U1 → owner liked contributor → MATCH Ma2 between U1 & P2 (owner U3)
        SwipeAction(id=SW6, swiper_id=U3, target_type="USER", target_id=str(U1),
                    action="LIKE", context_project_id=P2, context_role_slot_id=SLOT4,
                    context_key=str(P2), created_at=days_ago(5), is_active=True),

        # U7 swiped SUPER_LIKE on P2
        SwipeAction(id=SW7, swiper_id=U7, target_type="PROJECT", target_id=str(P2),
                    action="SUPER_LIKE", created_at=days_ago(1), is_active=True),
        # U3 swiped RIGHT on U7 → MATCH Ma3 between U7 & P2 (owner U3)
        SwipeAction(id=SW8, swiper_id=U3, target_type="USER", target_id=str(U7),
                    action="LIKE", context_project_id=P2, context_role_slot_id=SLOT4,
                    context_key=str(P2), created_at=days_ago(0), is_active=True),

        # ── P3 (LocalBites, owner U6) ──
        # U4 swiped RIGHT on P3
        SwipeAction(id=SW9, swiper_id=U4, target_type="PROJECT", target_id=str(P3),
                    action="LIKE", created_at=days_ago(8), is_active=True),
        # U6 swiped RIGHT on U4 → MATCH Ma4 between U4 & P3 (owner U6)
        SwipeAction(id=SW10, swiper_id=U6, target_type="USER", target_id=str(U4),
                    action="LIKE", context_project_id=P3, context_role_slot_id=SLOT6,
                    context_key=str(P3), created_at=days_ago(7), is_active=True),

        # U8 swiped RIGHT on P3 (waiting for owner to like back)
        SwipeAction(id=SW11, swiper_id=U8, target_type="PROJECT", target_id=str(P3),
                    action="LIKE", created_at=days_ago(2), is_active=True),

        # ── Owner mode swipes (U1 looking at contributors) ──
        SwipeAction(id=SW12, swiper_id=U1, target_type="USER", target_id=str(U3),
                    action="LIKE", context_project_id=P1, context_role_slot_id=SLOT2,
                    context_key=str(P1), created_at=days_ago(10), is_active=True),

        # U1 PASSED on U5 (passing a contributor)
        SwipeAction(id=SW13, swiper_id=U1, target_type="USER", target_id=str(U5),
                    action="PASS", context_project_id=P1, context_role_slot_id=SLOT2,
                    context_key=str(P1), created_at=days_ago(9), is_active=True),

        # ── Owner U3 looking at contributors ──
        SwipeAction(id=SW14, swiper_id=U3, target_type="USER", target_id=str(U2),
                    action="LIKE", context_project_id=P2, context_role_slot_id=SLOT5,
                    context_key=str(P2), created_at=days_ago(4), is_active=True),
        # U2 swiped RIGHT on P2 → U2 liked P2 → MATCH with owner U3 (already has Ma3)

        # Owner U3 PASSED on U5
        SwipeAction(id=SW15, swiper_id=U3, target_type="USER", target_id=str(U5),
                    action="PASS", context_project_id=P2, context_role_slot_id=SLOT4,
                    context_key=str(P2), created_at=days_ago(2), is_active=True),

        # ── Owner U6 looking at contributors ──
        SwipeAction(id=SW16, swiper_id=U6, target_type="USER", target_id=str(U1),
                    action="SUPER_LIKE", context_project_id=P3, context_role_slot_id=SLOT7,
                    context_key=str(P3), created_at=days_ago(3), is_active=True),
        SwipeAction(id=SW17, swiper_id=U6, target_type="USER", target_id=str(U2),
                    action="LIKE", context_project_id=P3, context_role_slot_id=SLOT6,
                    context_key=str(P3), created_at=days_ago(1), is_active=True),

        # ── Contributor U5 swiping on projects ──
        SwipeAction(id=SW18, swiper_id=U5, target_type="PROJECT", target_id=str(P3),
                    action="LIKE", created_at=days_ago(1), is_active=True),
    ]
    db.add_all(swipes)
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Matches & Chats
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_matches_and_chats(db: AsyncSession):
    await _seed_matches(db)
    await _seed_chats(db)
    await _seed_messages(db)


async def _seed_matches(db: AsyncSession):
    matches = [
        Match(id=MA1, user_id=U2, project_id=P1, owner_id=U1, role_matched="UI/UX Designer",
              match_score=92.5, matched_at=days_ago(4), is_unmatched=False, is_member_added=False),
        Match(id=MA2, user_id=U1, project_id=P2, owner_id=U3, role_matched="Full-Stack Developer",
              match_score=88.0, matched_at=days_ago(5), is_unmatched=False, is_member_added=False),
        Match(id=MA3, user_id=U7, project_id=P2, owner_id=U3, role_matched="DevOps Engineer",
              match_score=75.5, matched_at=days_ago(0), is_unmatched=False, is_member_added=False),
        Match(id=MA4, user_id=U4, project_id=P3, owner_id=U6, role_matched="Frontend Developer",
              match_score=90.0, matched_at=days_ago(7), is_unmatched=False, is_member_added=False),
    ]
    db.add_all(matches)
    await db.flush()


async def _seed_chats(db: AsyncSession):
    chats = [
        Chat(id=CH1, match_id=MA1, created_at=days_ago(4)),
        Chat(id=CH2, match_id=MA2, created_at=days_ago(5)),
        Chat(id=CH3, match_id=MA3, created_at=days_ago(0)),
        Chat(id=CH4, match_id=MA4, created_at=days_ago(7)),
    ]
    db.add_all(chats)
    await db.flush()


async def _seed_messages(db: AsyncSession):
    messages = [
        Message(id=MSG1,  chat_id=CH1, sender_id=U1, content="Hey Thu! I saw your portfolio, really impressive work on the EdTech dashboard design!", is_read=True,  created_at=days_ago(4)),
        Message(id=MSG2,  chat_id=CH1, sender_id=U2, content="Thank you Minh! I'm really excited about StudyMate. The AI-powered learning approach is exactly what I've been looking to work on.", is_read=True,  created_at=days_ago(4)),
        Message(id=MSG3,  chat_id=CH1, sender_id=U1, content="Would you be interested in joining as our UI/UX Designer? We need someone to design the student dashboard and onboarding flow.", is_read=True,  created_at=days_ago(3)),
        Message(id=MSG4,  chat_id=CH1, sender_id=U2, content="Absolutely! I'd love to. When can we start? I have some ideas for the onboarding flow already.", is_read=False, created_at=days_ago(2)),

        Message(id=MSG5,  chat_id=CH2, sender_id=U3, content="Hi Minh! Your full-stack experience would be perfect for GreenTrack. We need someone who can handle both frontend and backend.", is_read=True,  created_at=days_ago(5)),
        Message(id=MSG6,  chat_id=CH2, sender_id=U1, content="Thanks Khoa! I've actually been wanting to work on something climate-related. What's the tech stack?", is_read=True,  created_at=days_ago(5)),
        Message(id=MSG7,  chat_id=CH2, sender_id=U3, content="React Native for mobile, Go backend on AWS. We're also planning to use some IoT integrations.", is_read=False, created_at=days_ago(3)),

        Message(id=MSG8,  chat_id=CH4, sender_id=U6, content="Linh, your Flutter portfolio is solid! We're building a restaurant discovery app and could really use your mobile expertise.", is_read=True,  created_at=days_ago(7)),
        Message(id=MSG9,  chat_id=CH4, sender_id=U4, content="Hi Mai! I'd love to hear more. What's the monetization model? I have some experience with food-tech apps.", is_read=False, created_at=days_ago(6)),
        Message(id=MSG10, chat_id=CH3, sender_id=U3, content="Great to match Huy! Your DevOps skills are exactly what we need for setting up our CI/CD pipeline.", is_read=False, created_at=days_ago(0)),
    ]
    db.add_all(messages)
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Evaluations
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_evaluations(db: AsyncSession):
    evaluations = [
        ProjectEvaluation(
            id=EV1, project_id=P5, evaluator_id=U5, evaluatee_id=U7,
            quality_score=4.5, collaboration_score=4.0, communication_score=4.5,
            deadline_score=5.0, overall_score=4.5,
            feedback_text="Excellent work on the tokenizer and NER model. Always delivered ahead of deadlines. Communication was clear and proactive.",
            created_at=days_ago(5),
        ),
        ProjectEvaluation(
            id=EV2, project_id=P5, evaluator_id=U5, evaluatee_id=U8,
            quality_score=3.5, collaboration_score=4.0, communication_score=3.0,
            deadline_score=2.5, overall_score=3.3,
            feedback_text="Good work on the dataset curation, but had some delays. Communication could be improved - sometimes hard to reach.",
            created_at=days_ago(2),
        ),
    ]
    db.add_all(evaluations)
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Notifications
# ═══════════════════════════════════════════════════════════════════════════

async def _seed_notifications(db: AsyncSession):
    notifications = [
        Notification(id=N1, user_id=U2, type="NEW_MATCH", title="You have a new match!",
                     body="You matched with StudyMate! Start chatting.",
                     is_read=True, related_id=str(MA1), created_at=days_ago(4)),
        Notification(id=N2, user_id=U1, type="NEW_MATCH", title="You have a new match!",
                     body=f"A contributor matched with your project StudyMate!",
                     is_read=True, related_id=str(MA1), created_at=days_ago(4)),
        Notification(id=N3, user_id=U1, type="NEW_MATCH", title="You have a new match!",
                     body="You matched with GreenTrack! Start chatting.",
                     is_read=True, related_id=str(MA2), created_at=days_ago(5)),

        Notification(id=N4, user_id=U4, type="TASK_ASSIGNED",
                     title="New task assigned: Analytics dashboard",
                     body="Project owner Minh Nguyen assigned you a task in DevPort.",
                     is_read=False, related_id=str(T4), created_at=days_ago(15)),

        Notification(id=N5, user_id=U2, type="TASK_CHECKOUT",
                     title="Task confirmed: Design system component library",
                     body="Your checkout was confirmed as EARLY. Great job!",
                     is_read=True, related_id=str(T1), created_at=days_ago(10)),

        Notification(id=N6, user_id=U4, type="TASK_CHECKOUT",
                     title="Task confirmed: GitHub OAuth integration",
                     body="Your checkout was confirmed as ON_TIME.",
                     is_read=True, related_id=str(T2), created_at=days_ago(5)),

        Notification(id=N7, user_id=U2, type="NEW_MATCH",
                     title="You have a new match!",
                     body="You matched with LocalBites! Start chatting.",
                     is_read=False, related_id=str(MA4), created_at=days_ago(7)),
    ]
    db.add_all(notifications)
    await db.flush()


async def _seed_trust_safety(db: AsyncSession):
    db.add_all(
        [
            Report(
                id=uuid.UUID("e0000000-0000-0000-0000-000000000001"),
                reporter_id=U2,
                reported_id=U7,
                reason="Harassment",
                description="Seeded pending report for testing evidence review and reasoned resolution.",
                status="PENDING",
                context_type="CHAT",
                context_id=str(CH3),
                created_at=days_ago(1),
            ),
            UserBlock(
                id=uuid.UUID("e0000000-0000-0000-0000-000000000002"),
                blocker_id=U4,
                blocked_id=U11,
                reason="Seeded block scenario",
                created_at=days_ago(3),
            ),
        ]
    )
    await db.flush()


# ═══════════════════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════════════════

async def main():
    engine = create_async_engine(ASYNC_ENGINE_URL, echo=False)
    try:
        await seed(engine)
    finally:
        await engine.dispose()


if __name__ == "__main__":
    asyncio.run(main())
