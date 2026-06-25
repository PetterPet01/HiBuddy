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


def create_role(name, skills):
    """Helper to create a mock role with role_skills."""
    return SimpleNamespace(
        role_name=name,
        role_skills=[
            SimpleNamespace(
                level=level,
                skill=SimpleNamespace(name=skill_name),
            )
            for skill_name, level in skills
        ],
    )


def create_requirement(name, level, required=True):
    """Helper to create a skill requirement."""
    return SimpleNamespace(
        minimum_level=level,
        is_required=required,
        skill=SimpleNamespace(name=name),
    )


def test_matching_scenarios_and_cosine_similarity():
    """Test matching score and cosine similarity score for 5 different user profiles
    against a target project requirement using in-memory mock objects.
    """
    # 1. Setup Project & Owner
    owner = SimpleNamespace(
        created_at=datetime.now(timezone.utc) - timedelta(days=100),
        verified_student=False,
    )
    owner_profile = SimpleNamespace(
        user=owner,
        display_name="Project Owner",
        bio="Owner of HiBuddy team",
        reputation_score=4.0,
        projects_completed=5,
    )
    owner.profile = owner_profile

    # Target Project
    backend_slot = SimpleNamespace(
        id=uuid4(),
        role_name="Backend Developer",
        count=1,
        filled=0,
        skill_requirements=None,
        skill_requirements_rows=[
            create_requirement("Python", "INTERMEDIATE", required=True)
        ],
    )
    frontend_slot = SimpleNamespace(
        id=uuid4(),
        role_name="Frontend Developer",
        count=1,
        filled=0,
        skill_requirements=None,
        skill_requirements_rows=[
            create_requirement("React", "INTERMEDIATE", required=True)
        ],
    )

    project = SimpleNamespace(
        title="E-Commerce Web Platform",
        field="Web",
        description="Developing a modern web platform for e-commerce using Python, PostgreSQL, React.",
        specific_goal="Launch a robust web application MVP",
        work_mode="ONLINE",
        commitment_level="CASUAL",
        created_at=datetime.now(timezone.utc) - timedelta(days=2),  # Recency bonus
        role_slots=[backend_slot, frontend_slot],
    )

    # 2. Create the 5 User Profiles
    # Profile 1: Newbie, role matches Backend Developer (matches Python)
    user1 = SimpleNamespace(
        full_name="Newbie Python Dev",
        verified_student=False,
        created_at=datetime.now(timezone.utc) - timedelta(days=5),
        university="HUST",
    )
    profile1 = SimpleNamespace(
        user=user1,
        display_name="Newbie Python Dev",
        bio="Enthusiastic beginner backend developer looking for a project.",
        mode="CONTRIBUTOR",
        reputation_score=0.0,
        projects_completed=0,
        short_term_goal="Gain real experience",
    )
    user1.profile = profile1
    user1.roles = [create_role("Backend Developer", [("Python", "ADVANCED")])]
    user1.skills = [SimpleNamespace(skill_name="Python", level="ADVANCED")]
    user1.interests = [SimpleNamespace(interest_name="Web")]

    # Profile 2: Normal user, matches both Backend and Frontend Developer
    user2 = SimpleNamespace(
        full_name="Fullstack Dev",
        verified_student=False,
        created_at=datetime.now(timezone.utc) - timedelta(days=60),
        university="NEU",
    )
    profile2 = SimpleNamespace(
        user=user2,
        display_name="Experienced Fullstack",
        bio="Fullstack developer with experience in python and react web platforms.",
        mode="CONTRIBUTOR",
        reputation_score=4.5,
        projects_completed=5,
        short_term_goal="Build quality software",
    )
    user2.profile = profile2
    user2.roles = [
        create_role("Backend Developer", [("Python", "ADVANCED")]),
        create_role("Frontend Developer", [("React", "INTERMEDIATE")]),
    ]
    user2.skills = [
        SimpleNamespace(skill_name="Python", level="ADVANCED"),
        SimpleNamespace(skill_name="React", level="INTERMEDIATE"),
    ]
    user2.interests = [SimpleNamespace(interest_name="Web")]

    # Profile 3: Normal user, non-matching role and field (Mobile Dev, Swift, Mobile)
    user3 = SimpleNamespace(
        full_name="iOS Dev",
        verified_student=False,
        created_at=datetime.now(timezone.utc) - timedelta(days=90),
        university="FTU",
    )
    profile3 = SimpleNamespace(
        user=user3,
        display_name="Mobile Dev",
        bio="Experienced iOS developer focused on mobile apps.",
        mode="CONTRIBUTOR",
        reputation_score=4.2,
        projects_completed=4,
        short_term_goal="Work on mobile solutions",
    )
    user3.profile = profile3
    user3.roles = [create_role("Mobile Developer", [("Swift", "ADVANCED")])]
    user3.skills = [SimpleNamespace(skill_name="Swift", level="ADVANCED")]
    user3.interests = [SimpleNamespace(interest_name="Mobile")]

    # Profile 4: Verified Student, non-matching role (UI/UX Designer, Figma, Design)
    user4 = SimpleNamespace(
        full_name="Student Designer",
        verified_student=True,
        created_at=datetime.now(timezone.utc) - timedelta(days=45),
        university="UET",
    )
    profile4 = SimpleNamespace(
        user=user4,
        display_name="Student Designer",
        bio="Student designer eager to work on mobile UI.",
        mode="CONTRIBUTOR",
        reputation_score=4.0,
        projects_completed=2,
        short_term_goal="Learn UI designs",
    )
    user4.profile = profile4
    user4.roles = [create_role("UI/UX Designer", [("Figma", "INTERMEDIATE")])]
    user4.skills = [SimpleNamespace(skill_name="Figma", level="INTERMEDIATE")]
    user4.interests = [SimpleNamespace(interest_name="Design")]

    # Profile 5: Newbie, non-matching role (UI/UX Designer, Figma, Design)
    user5 = SimpleNamespace(
        full_name="Newbie Designer",
        verified_student=False,
        created_at=datetime.now(timezone.utc) - timedelta(days=10),
        university="RMIT",
    )
    profile5 = SimpleNamespace(
        user=user5,
        display_name="Newbie Designer",
        bio="New UI/UX designer interested in graphics.",
        mode="CONTRIBUTOR",
        reputation_score=0.0,
        projects_completed=0,
        short_term_goal="Explore UI/UX projects",
    )
    user5.profile = profile5
    user5.roles = [create_role("UI/UX Designer", [("Figma", "ADVANCED")])]
    user5.skills = [SimpleNamespace(skill_name="Figma", level="ADVANCED")]
    user5.interests = [SimpleNamespace(interest_name="Design")]

    profiles = [
        ("Newbie matching role", user1),
        ("Normal double-matching role", user2),
        ("Normal non-matching role & field", user3),
        ("Verified student non-matching role", user4),
        ("Newbie non-matching role", user5),
    ]

    print("\n" + "="*80)
    print(f"MATCHING SCORING RESULTS FOR PROJECT: '{project.title}'")
    print("="*80)

    with patch("app.services.embedding_service.encode_text", side_effect=mock_encode_text):
        project_text = build_project_text(project)
        project_vector = mock_encode_text(project_text)

        for label, user in profiles:
            # Calculate deterministic score
            score, explanation, matched_slot = calculate_project_score_details(user, project, owner)
            
            # Calculate cosine similarity
            user_text = build_user_text(user.profile)
            user_vector = mock_encode_text(user_text)
            cosine_sim = _cosine_similarity(user_vector, project_vector)
            
            print(f"Profile: {label} ({user.full_name})")
            print(f"  - Matching Score: {score}/100")
            print(f"  - Cosine Similarity: {cosine_sim:.4f}")
            print(f"  - Matched Role Slot: {matched_slot.role_name if matched_slot else 'None'}")
            print(f"  - Explanation Details: {explanation}")
            print("-" * 50)

            # Assertions based on rules
            if label == "Newbie matching role":
                assert score > 50.0
                assert matched_slot is backend_slot
                assert explanation["slot"]["role_fit"] == 100.0
            elif label == "Normal double-matching role":
                assert score > 70.0
                assert matched_slot in [backend_slot, frontend_slot]
                assert explanation["slot"]["role_fit"] == 100.0
            elif label == "Normal non-matching role & field":
                assert score < 30.0
                assert explanation["slot"]["role_fit"] == 0.0
            elif label == "Verified student non-matching role":
                assert score < 35.0
                assert explanation["slot"]["role_fit"] == 0.0
            elif label == "Newbie non-matching role":
                assert score < 30.0
                assert explanation["slot"]["role_fit"] == 0.0

    print("="*80 + "\n")
