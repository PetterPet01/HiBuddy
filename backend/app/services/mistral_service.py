"""Call Mistral AI to analyze feedback and extract skill weaknesses, and moderate project content."""
import json
import logging
from typing import Any

import httpx

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

MISTRAL_URL = "https://mistral.24102006.xyz/v1/chat/completions"

SYSTEM_PROMPT = """You are a skill gap analyzer. Given anonymous peer feedback about a team member, identify the specific technical or soft skills the person is weak at and should improve.

Rules:
- Return ONLY a JSON array of skill name strings.
- Extract concrete skills (e.g., "React", "Docker", "Communication").
- Max 5 skills. If none, return [].

Example: "A code React component hay re-render, state chua tot" → ["React", "State Management", "Performance Optimization"]
Example: "B giao tiep kem, khong bao gio bao cao tien do" → ["Communication", "Time Management"]
Example: "Ban la nguoi giao tiep tot, nhanh nhay, luon lam tot trong moi cong viec" → []"""
MODERATION_SYSTEM_PROMPT = """You are a content moderation assistant. Given a project posting (title, description, goals, requirements), determine if the content violates community standards.

Violation categories:
- spam: Promotional content, irrelevant links, repetitive content
- fraud: Scams, deceptive offers, false promises, impersonation
- offensive: Profanity, hate speech, discriminatory language, harassment
- provocative: Trolling, inflammatory content, baiting
- hostile: Threats, intimidation, violent content

Rules:
- Return ONLY a JSON object with this exact structure: {"is_flagged": bool, "categories": [string], "reasons": [string]}
- is_flagged: true if ANY violation category is detected
- categories: list of violation category strings detected (empty if not flagged)
- reasons: brief specific explanation for each flagged violation (empty if not flagged)
- Be conservative — only flag content that clearly violates standards. Err on the side of allowing content."""


async def _call_mistral(system_prompt: str, user_prompt: str, max_tokens: int = 300) -> str | None:
    if not settings.MISTRAL_API_KEY:
        logger.warning("MISTRAL_API_KEY not configured")
        return None

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                MISTRAL_URL,
                headers={
                    "Authorization": f"Bearer {settings.MISTRAL_API_KEY}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": settings.MISTRAL_MODEL,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    "temperature": 0.1,
                    "max_tokens": max_tokens,
                },
            )
            response.raise_for_status()
            content = response.json()["choices"][0]["message"]["content"].strip()
            if content.startswith("```"):
                content = content.split("\n", 1)[1].rsplit("\n", 1)[0]
            return content
    except Exception as e:
        logger.error(f"Mistral call failed: {e}")
        return None


async def analyze_feedback_weaknesses(feedback_text: str) -> list[str]:
    content = await _call_mistral(SYSTEM_PROMPT, f"Analyze this peer feedback:\n\n{feedback_text}", 150)
    if not content:
        return []

    try:
        result = json.loads(content)
        if isinstance(result, list):
            return [s for s in result if isinstance(s, str)][:5]
        return []
    except Exception as e:
        logger.error(f"Failed to parse Mistral feedback analysis: {e}")
        return []


async def moderate_project_content(title: str, description: str, specific_goal: str | None = None, additional_requirements: str | None = None, member_benefits: str | None = None) -> dict[str, Any]:
    """Check project content for community standards violations using Mistral.

    Returns {"is_flagged": bool, "categories": list[str], "reasons": list[str]}.
    On error or missing API key, returns {"is_flagged": False, "categories": [], "reasons": []} (fail-open).
    """
    if not settings.MISTRAL_API_KEY:
        logger.warning("MISTRAL_API_KEY not configured — skipping moderation")
        return {"is_flagged": False, "categories": [], "reasons": []}

    project_text = f"Title: {title}\nDescription: {description}\n"
    if specific_goal:
        project_text += f"Specific Goal: {specific_goal}\n"
    if additional_requirements:
        project_text += f"Additional Requirements: {additional_requirements}\n"
    if member_benefits:
        project_text += f"Member Benefits: {member_benefits}\n"

    content = await _call_mistral(MODERATION_SYSTEM_PROMPT, f"Review this project posting for community standards violations:\n\n{project_text}", 300)
    if not content:
        return {"is_flagged": False, "categories": [], "reasons": []}

    try:
        result = json.loads(content)
        return {
            "is_flagged": bool(result.get("is_flagged", False)),
            "categories": result.get("categories", []),
            "reasons": result.get("reasons", []),
        }
    except Exception as e:
        logger.error(f"Failed to parse Mistral moderation result: {e}")
        return {"is_flagged": False, "categories": [], "reasons": []}
