from datetime import datetime, timedelta, timezone
from types import SimpleNamespace
from uuid import uuid4
from unittest.mock import patch

from app.services.matching_service import calculate_project_score_details
from app.services.swipe_service import _cosine_similarity
from app.services.embedding_service import build_user_text, build_project_text


def mock_encode_text(text: str) -> list[float]:
    """Mock text encoding using a simple bag-of-words hashing vectorizer.
    
    This ensures tests can run without downloading HuggingFace models offline.
    """
    import hashlib
    vector = [0.0] * 384
    words = text.lower().split()
    for word in words:
        clean_word = "".join(c for c in word if c.isalnum())
        if not clean_word:
            continue
        h = int(hashlib.md5(clean_word.encode("utf-8")).hexdigest(), 16)
        index = h % 384
        vector[index] += 1.0
    return vector


def role(name, skills=None):
    return SimpleNamespace(
        role_name=name,
        role_skills=[
            SimpleNamespace(level=level, skill=SimpleNamespace(name=skill_name))
            for skill_name, level in (skills or [])
        ],
    )


def requirement(name, level, required=True):
    return SimpleNamespace(
        minimum_level=level,
        is_required=required,
        skill=SimpleNamespace(name=name),
    )


def make_owner(reputation_score=0.0, projects_completed=0, verified_student=False):
    owner = SimpleNamespace(
        verified_student=verified_student,
        created_at=datetime.now(timezone.utc) - timedelta(days=60),
        profile=SimpleNamespace(
            reputation_score=reputation_score,
            projects_completed=projects_completed,
        ),
    )
    owner.profile.user = owner
    return owner


def make_user(name, *, role_name="Backend Developer", skills=None, verified_student=False):
    user = SimpleNamespace(
        full_name=name,
        verified_student=verified_student,
        created_at=datetime.now(timezone.utc) - timedelta(days=20),
        university="HUST",
        roles=[role(role_name, skills or [])],
        skills=[SimpleNamespace(skill_name=s[0], level=s[1]) for s in (skills or [])],
        interests=[SimpleNamespace(interest_name="Web")],
        profile=SimpleNamespace(
            reputation_score=0.0,
            projects_completed=0,
            mode="CONTRIBUTOR",
            display_name=name,
            bio=f"Enthusiastic {role_name} developer.",
            short_term_goal="Contribute to projects",
        ),
    )
    user.profile.user = user
    return user


def test_swipe_project_scenarios_with_varied_owner_quality():
    slot = SimpleNamespace(
        id=uuid4(),
        role_name="Backend Developer",
        count=1,
        filled=0,
        skill_requirements=None,
        skill_requirements_rows=[
            requirement("Python", "INTERMEDIATE", required=True),
            requirement("PostgreSQL", "BEGINNER", required=False),
        ],
    )
    project = SimpleNamespace(
        title="AI Study Platform",
        field="Web",
        description="Build a modern web app with Python, PostgreSQL, and AI features.",
        specific_goal="Launch MVP",
        work_mode="ONLINE",
        commitment_level="CASUAL",
        created_at=datetime.now(timezone.utc) - timedelta(days=3),
        role_slots=[slot],
    )

    cases = [
        (
            "high_fit_high_owner_quality",
            make_user("Alice", role_name="Backend Developer", skills=[("Python", "ADVANCED")]),
            make_owner(reputation_score=4.8, projects_completed=8, verified_student=True),
        ),
        (
            "high_fit_low_owner_quality",
            make_user("Bob", role_name="Backend Developer", skills=[("Python", "ADVANCED")]),
            make_owner(reputation_score=1.2, projects_completed=1, verified_student=False),
        ),
        (
            "partial_fit_high_owner_quality",
            make_user("Cara", role_name="Backend Developer", skills=[("Java", "ADVANCED")]),
            make_owner(reputation_score=4.5, projects_completed=6, verified_student=True),
        ),
        (
            "no_fit_verified_owner",
            make_user("Duy", role_name="UI/UX Designer", skills=[("Figma", "ADVANCED")]),
            make_owner(reputation_score=4.8, projects_completed=9, verified_student=True),
        ),
        (
            "no_fit_unverified_owner",
            make_user("Eli", role_name="UI/UX Designer", skills=[("Figma", "ADVANCED")]),
            make_owner(reputation_score=1.0, projects_completed=0, verified_student=False),
        ),
    ]

    results = {}
    print("\n" + "="*80)
    print(f"MATCHING SCORING RESULTS FOR PROJECT SCENARIOS")
    print("="*80)

    with patch("app.services.embedding_service.encode_text", side_effect=mock_encode_text):
        project_text = build_project_text(project)
        project_vector = mock_encode_text(project_text)
        
        detail_results = []
        for label, user, owner in cases:
            score, explanation, matched_slot = calculate_project_score_details(user, project, owner)
            assert matched_slot is slot or matched_slot is None
            assert 0.0 <= explanation["factors"]["owner_quality"] <= 100.0
            results[label] = score
            
            user_text = build_user_text(user.profile)
            user_vector = mock_encode_text(user_text)
            cosine_sim = _cosine_similarity(user_vector, project_vector)
            
            detail_results.append({
                "label": label,
                "user": user,
                "owner": owner,
                "score": score,
                "cosine_sim": cosine_sim,
                "matched_slot": matched_slot,
                "explanation": explanation
            })
            
        # Rank by score (descending)
        detail_results.sort(key=lambda x: x["score"], reverse=True)
        
        for idx, res in enumerate(detail_results):
            label = res["label"]
            user = res["user"]
            owner = res["owner"]
            score = res["score"]
            cosine_sim = res["cosine_sim"]
            matched_slot = res["matched_slot"]
            explanation = res["explanation"]
            
            print(f"Rank {idx + 1}: {label} ({user.full_name})")
            print(f"  - Matching Score: {score}/100")
            print(f"  - Cosine Similarity: {cosine_sim:.4f}")
            print(f"  - Owner Profile: Rep={owner.profile.reputation_score}, Projects Completed={owner.profile.projects_completed}, Verified={owner.verified_student}")
            print(f"  - Matched Role Slot: {matched_slot.role_name if matched_slot else 'None'}")
            print(f"  - Explanation Details: {explanation}")
            print("-" * 50)
            
    print("="*80 + "\n")

    assert results["high_fit_high_owner_quality"] > results["high_fit_low_owner_quality"]
    assert results["partial_fit_high_owner_quality"] > results["no_fit_unverified_owner"]
    assert results["high_fit_high_owner_quality"] > results["partial_fit_high_owner_quality"]
    assert results["high_fit_high_owner_quality"] > results["no_fit_verified_owner"]
