import logging
from collections import defaultdict
from typing import Any
from uuid import UUID

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from app.models.task import Task, TaskCheckoutHistory, ProjectEvaluation
from app.models.chat import CourseSuggestion
from app.models.profile import UserSkill, UserProfile, UserRole
from app.models.feedback import AnonymousFeedback
from app.models.user import User
from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

COURSE_CATALOG: list[dict[str, Any]] = [
    {
        "course_id": "coursera-google-ux",
        "title": "Google UX Design Certificate",
        "source": "Coursera",
        "url": "https://www.coursera.org/professional-certificates/google-ux-design",
        "skills": ["UI/UX Design", "Design", "Wireframing"],
        "level": "Intermediate",
        "duration_hours": 240,
    },
    {
        "course_id": "udemy-communication",
        "title": "Khóa học Thuyết trình & giao tiếp nhóm",
        "source": "Udemy",
        "url": "https://www.udemy.com/course/presentation-communication",
        "skills": ["Communication", "Presentation", "Teamwork"],
        "level": "Beginner",
        "duration_hours": 12,
    },
    {
        "course_id": "freecodecamp-git",
        "title": "Git & GitHub for Beginners",
        "source": "freeCodeCamp",
        "url": "https://www.freecodecamp.org/news/git-and-github-for-beginners",
        "skills": ["Git", "GitHub", "Version Control"],
        "level": "Beginner",
        "duration_hours": 4,
    },
    {
        "course_id": "coursera-ielts",
        "title": "IELTS Band 7.0 Preparation",
        "source": "IDP Education",
        "url": "https://ielts.idp.com/prepare",
        "skills": ["English", "IELTS", "Communication"],
        "level": "Intermediate",
        "duration_hours": 80,
    },
    {
        "course_id": "udemy-react",
        "title": "React - The Complete Guide 2024",
        "source": "Udemy",
        "url": "https://www.udemy.com/course/react-the-complete-guide",
        "skills": ["React", "JavaScript", "Frontend"],
        "level": "Beginner",
        "duration_hours": 48,
    },
    {
        "course_id": "coursera-python",
        "title": "Python for Everybody",
        "source": "Coursera",
        "url": "https://www.coursera.org/specializations/python",
        "skills": ["Python", "Programming", "Backend"],
        "level": "Beginner",
        "duration_hours": 80,
    },
    {
        "course_id": "youtube-docker",
        "title": "Docker Tutorial for Beginners",
        "source": "YouTube",
        "url": "https://www.youtube.com/watch?v=fqMOX6JJhGo",
        "skills": ["Docker", "DevOps", "Containerization"],
        "level": "Beginner",
        "duration_hours": 2,
    },
    {
        "course_id": "coursera-mldl",
        "title": "Machine Learning Specialization",
        "source": "Coursera",
        "url": "https://www.coursera.org/specializations/machine-learning-introduction",
        "skills": ["Machine Learning", "AI", "Python", "Data Science"],
        "level": "Intermediate",
        "duration_hours": 120,
    },
    {
        "course_id": "udemy-kotlin",
        "title": "Kotlin for Android Development",
        "source": "Udemy",
        "url": "https://www.udemy.com/course/kotlin-android",
        "skills": ["Kotlin", "Android", "Mobile Development"],
        "level": "Beginner",
        "duration_hours": 32,
    },
    {
        "course_id": "freecodecamp-dsa",
        "title": "Data Structures & Algorithms",
        "source": "freeCodeCamp",
        "url": "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures",
        "skills": ["Algorithms", "Data Structures", "Problem Solving"],
        "level": "Intermediate",
        "duration_hours": 300,
    },
    {
        "course_id": "coursera-project-mgmt",
        "title": "Project Management Principles",
        "source": "Coursera",
        "url": "https://www.coursera.org/learn/project-management",
        "skills": ["Project Management", "Leadership", "Planning"],
        "level": "Beginner",
        "duration_hours": 20,
    },
    {
        "course_id": "udemy-nodejs",
        "title": "Node.js - The Complete Guide",
        "source": "Udemy",
        "url": "https://www.udemy.com/course/nodejs-the-complete-guide",
        "skills": ["Node.js", "Express", "Backend", "JavaScript"],
        "level": "Beginner",
        "duration_hours": 40,
    },
    {
        "course_id": "youtube-figma",
        "title": "Figma UI/UX Design Tutorial",
        "source": "YouTube",
        "url": "https://www.youtube.com/watch?v=jwCmIBJ8Jtc",
        "skills": ["Figma", "UI Design", "Prototyping"],
        "level": "Beginner",
        "duration_hours": 1.5,
    },
    {
        "course_id": "coursera-agile",
        "title": "Agile & Scrum Fundamentals",
        "source": "Coursera",
        "url": "https://www.coursera.org/learn/agile-scrum",
        "skills": ["Agile", "Scrum", "Project Management", "Teamwork"],
        "level": "Beginner",
        "duration_hours": 15,
    },
    {
        "course_id": "udemy-flutter",
        "title": "Flutter & Dart - Complete Development",
        "source": "Udemy",
        "url": "https://www.udemy.com/course/flutter-dart",
        "skills": ["Flutter", "Dart", "Mobile Development"],
        "level": "Beginner",
        "duration_hours": 42,
    },
]


async def _get_feedback_weaknesses(db: AsyncSession, user_id: UUID) -> dict[str, float]:
    result = await db.execute(
        select(AnonymousFeedback).where(
            AnonymousFeedback.target_id == user_id,
            AnonymousFeedback.analyzed_weaknesses.isnot(None),
        )
    )
    scores: dict[str, float] = defaultdict(float)
    for fb in result.scalars():
        if fb.analyzed_weaknesses:
            for skill in fb.analyzed_weaknesses:
                scores[skill] += 0.4
    return dict(scores)


async def generate_course_suggestions(db: AsyncSession, user_id: UUID) -> list[dict]:
    skill_scores: dict[str, float] = defaultdict(float)

    late_tasks_result = await db.execute(
        select(Task).where(
            Task.assignee_id == user_id,
            Task.checkout_status == "LATE",
        )
    )
    for task in late_tasks_result.scalars():
        if task.role_related:
            skill_scores[task.role_related] += 0.5

    low_eval_result = await db.execute(
        select(ProjectEvaluation).where(
            ProjectEvaluation.evaluatee_id == user_id,
            ProjectEvaluation.overall_score < 3.0,
        )
    )
    for evaluation in low_eval_result.scalars():
        if evaluation.quality_score < 3.0:
            skill_scores["Technical Quality"] += 0.3
        if evaluation.collaboration_score < 3.0:
            skill_scores["Teamwork"] += 0.3
        if evaluation.communication_score < 3.0:
            skill_scores["Communication"] += 0.3
        if evaluation.deadline_score < 3.0:
            skill_scores["Time Management"] += 0.3

    profile_skills_result = await db.execute(
        select(UserSkill).where(
            UserSkill.user_id == user_id,
            UserSkill.needs_improvement == True,
        )
    )
    for skill in profile_skills_result.scalars():
        skill_scores[skill.skill_name] += 0.2

    fb_weaknesses = await _get_feedback_weaknesses(db, user_id)
    for skill, weight in fb_weaknesses.items():
        skill_scores[skill] += weight

    dismissed_result = await db.execute(
        select(CourseSuggestion.course_id).where(
            CourseSuggestion.user_id == user_id,
            CourseSuggestion.is_dismissed == True,
        )
    )
    dismissed_ids = set(r[0] for r in dismissed_result.all())

    suggestions = []
    for course in COURSE_CATALOG:
        if course["course_id"] in dismissed_ids:
            continue

        match_score = 0.0
        for skill in course["skills"]:
            if skill in skill_scores:
                match_score += skill_scores[skill] * 100
            skill_lower = skill.lower()
            for ks, kv in skill_scores.items():
                if skill_lower in ks.lower() or ks.lower() in skill_lower:
                    match_score += kv * 50

        if match_score > 0:
            suggestions.append({
                "course_id": course["course_id"],
                "course_title": course["title"],
                "source": course["source"],
                "url": course["url"],
                "target_skill": ", ".join(course["skills"]),
                "match_percent": min(round(match_score, 1), 99.0),
            })

    suggestions.sort(key=lambda s: s["match_percent"], reverse=True)

    existing = await db.execute(
        select(CourseSuggestion).where(CourseSuggestion.user_id == user_id)
    )
    existing_suggestions = existing.scalars().all()

    result = []
    for s in suggestions[:settings.MAX_COURSE_SUGGESTIONS]:
        found = False
        for es in existing_suggestions:
            if es.course_id == s["course_id"] and not es.is_dismissed:
                es.match_percent = s["match_percent"]
                result.append({
                    "id": str(es.id),
                    "target_skill": es.target_skill,
                    "course_title": es.course_title,
                    "course_id": es.course_id,
                    "source": es.source,
                    "url": es.url,
                    "match_percent": es.match_percent,
                    "is_dismissed": es.is_dismissed,
                })
                found = True
                break
        if not found:
            new_suggestion = CourseSuggestion(
                user_id=user_id,
                target_skill=s["target_skill"],
                course_title=s["course_title"],
                course_id=s["course_id"],
                source=s["source"],
                url=s["url"],
                match_percent=s["match_percent"],
            )
            db.add(new_suggestion)
            await db.flush()
            result.append({
                "id": str(new_suggestion.id),
                "target_skill": new_suggestion.target_skill,
                "course_title": new_suggestion.course_title,
                "course_id": new_suggestion.course_id,
                "source": new_suggestion.source,
                "url": new_suggestion.url,
                "match_percent": new_suggestion.match_percent,
                "is_dismissed": new_suggestion.is_dismissed,
            })

    return result


async def suggest_mentors(db: AsyncSession, user_id: UUID) -> list[dict]:
    user_skills_result = await db.execute(
        select(UserSkill).where(UserSkill.user_id == user_id)
    )
    user_skills = {s.skill_name.lower() for s in user_skills_result.scalars()}

    profiles_result = await db.execute(
        select(UserProfile).where(
            UserProfile.reputation_score >= 4.0,
            UserProfile.mode.in_(["CONTRIBUTOR", "BOTH"]),
            UserProfile.is_hidden == False,
            UserProfile.user_id != user_id,
        ).limit(20)
    )
    profiles = profiles_result.scalars().all()

    mentors = []
    for profile in profiles:
        profile_user = await db.get(User, profile.user_id)
        profile_skills_result = await db.execute(
            select(UserSkill).where(UserSkill.user_id == profile.user_id)
        )
        profile_skills = {s.skill_name.lower() for s in profile_skills_result.scalars()}
        profile_roles_result = await db.execute(
            select(UserRole).where(UserRole.user_id == profile.user_id)
        )

        overlap = user_skills & profile_skills
        total = user_skills | profile_skills
        match = len(overlap) / len(total) * 100 if total else 0

        mentors.append({
            "user_id": str(profile.user_id),
            "display_name": profile.display_name,
            "avatar_url": profile_user.avatar_url if profile_user else None,
            "verified_student": profile_user.verified_student if profile_user else False,
            "university": profile_user.university if profile_user else None,
            "bio": profile.bio,
            "roles": [{"role_name": r.role_name} for r in profile_roles_result.scalars()],
            "skills": [{"skill_name": s.skill_name, "level": s.level} for s in profile_skills_result.scalars()],
            "reputation_score": profile.reputation_score,
            "match_score": round(match, 1),
        })

    mentors.sort(key=lambda m: m["match_score"], reverse=True)
    return mentors[:10]


async def dismiss_course_suggestion(db: AsyncSession, user_id: UUID, suggestion_id: str) -> dict:
    result = await db.execute(
        select(CourseSuggestion).where(
            CourseSuggestion.id == UUID(suggestion_id),
            CourseSuggestion.user_id == user_id,
        )
    )
    suggestion = result.scalar_one_or_none()
    if not suggestion:
        raise Exception("Suggestion not found")
    suggestion.is_dismissed = True
    return {"message": "Suggestion dismissed"}


async def refresh_suggestions(db: AsyncSession, user_id: UUID) -> list[dict]:
    result = await db.execute(
        select(CourseSuggestion).where(CourseSuggestion.user_id == user_id)
    )
    for s in result.scalars():
        await db.delete(s)
    await db.flush()
    return await generate_course_suggestions(db, user_id)
