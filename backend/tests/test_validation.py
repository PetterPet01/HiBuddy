from datetime import date

import pytest
from pydantic import ValidationError

from app.schemas.auth import ResetPassword, UserRegister
from app.schemas.profile import ProfileUpdate
from app.schemas.project import ProjectCreate


def registration_payload(**overrides):
    today = date.today()
    adult_birth = today.replace(year=today.year - 18)
    payload = {
        "full_name": "Test User",
        "username": "Test.User",
        "email": "test@example.com",
        "date_of_birth": adult_birth.strftime("%d/%m/%Y"),
        "password": "StrongPass1",
        "confirm_password": "StrongPass1",
        "agree_terms": True,
    }
    payload.update(overrides)
    return payload


def test_registration_normalizes_username_and_accepts_exactly_18():
    data = UserRegister(**registration_payload())
    assert data.username == "test.user"


def test_registration_accepts_allowed_username_symbols():
    data = UserRegister(**registration_payload(username="Test.User,_@1"))
    assert data.username == "test.user,_@1"


@pytest.mark.parametrize("phone", ["0912345678", "+84912345678", ""])
def test_registration_accepts_valid_optional_phone(phone):
    data = UserRegister(**registration_payload(phone=phone))
    assert data.phone == (phone or None)


@pytest.mark.parametrize(
    "overrides",
    [
        {"password": "weakpass1", "confirm_password": "weakpass1"},
        {"agree_terms": False},
        {"confirm_password": "Different1"},
        {"full_name": "Test User1"},
        {"full_name": "Test@User"},
        {"username": "test user"},
        {"username": "te"},
        {"username": "test-user"},
        {"email": "invalid-email"},
        {"email": "invalid@example"},
        {"date_of_birth": "01/01/1900"},
        {"date_of_birth": date.today().strftime("%d/%m/%Y")},
        {"phone": "091234567"},
        {"phone": "09123456789"},
        {"phone": "phone-number"},
    ],
)
def test_registration_rejects_invalid_constraints(overrides):
    with pytest.raises(ValidationError):
        UserRegister(**registration_payload(**overrides))


def test_reset_password_enforces_bcrypt_byte_limit():
    long_password = "A1" + ("é" * 40)
    with pytest.raises(ValidationError):
        ResetPassword(
            email="test@example.com",
            code="123456",
            new_password=long_password,
            confirm_password=long_password,
        )


def test_project_capacity_includes_owner():
    with pytest.raises(ValidationError):
        ProjectCreate(
            title="Capacity test",
            field="Web",
            description="A sufficiently detailed project description.",
            start_date="2026-07-01",
            end_date="2026-08-01",
            max_members=2,
            role_slots=[{"role_name": "Developer", "count": 2}],
        )


def test_profile_accepts_distinct_skills_per_role():
    profile = ProfileUpdate(
        display_name="Scoped Skills",
        roles=[
            {
                "role_name": "Backend Developer",
                "ordering": 0,
                "skills": [{"skill_name": "PostgreSQL", "level": "ADVANCED"}],
            },
            {
                "role_name": "UI/UX Designer",
                "ordering": 1,
                "skills": [{"skill_name": "Figma", "level": "INTERMEDIATE"}],
            },
        ],
        interests=["EdTech"],
    )
    assert profile.roles[0].skills[0].skill_name == "PostgreSQL"
    assert profile.roles[1].skills[0].skill_name == "Figma"
