from datetime import datetime
from uuid import UUID
import re

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator


class UserRegister(BaseModel):
    full_name: str
    username: str
    email: EmailStr
    date_of_birth: str
    password: str
    confirm_password: str
    phone: str | None = None
    agree_terms: bool

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        v = v.strip()
        if len(v) < 2:
            raise ValueError("Full name is too short")
        if any(not (char.isalpha() or char.isspace()) for char in v):
            raise ValueError("Full name must contain only letters and spaces")
        return v

    @field_validator("username")
    @classmethod
    def validate_username(cls, v: str) -> str:
        v = v.strip().lower()
        if len(v) < 3:
            raise ValueError("Username must be at least 3 characters")
        if re.search(r"\s", v):
            raise ValueError("Username cannot contain spaces")
        if not re.fullmatch(r"[a-zA-Z0-9._,@]+", v):
            raise ValueError("Username can only contain letters, numbers, '.', ',', '_', '@'")
        return v

    @field_validator("password")
    @classmethod
    def validate_password(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        if not re.search(r"[A-Z]", v):
            raise ValueError("Password must contain at least one uppercase letter")
        if not re.search(r"[a-z]", v):
            raise ValueError("Password must contain at least one lowercase letter")
        if not re.search(r"\d", v):
            raise ValueError("Password must contain at least one digit")
        if len(v.encode("utf-8")) > 72:
            raise ValueError("Password must be at most 72 bytes")
        return v

    @field_validator("confirm_password")
    @classmethod
    def passwords_match(cls, v: str, info) -> str:
        if "password" in info.data and v != info.data["password"]:
            raise ValueError("Passwords do not match")
        return v

    @field_validator("date_of_birth")
    @classmethod
    def validate_dob(cls, v: str) -> str:
        try:
            dt = datetime.strptime(v, "%d/%m/%Y")
        except ValueError:
            raise ValueError("Date of birth must be in DD/MM/YYYY format")
        today = datetime.now().date()
        if dt <= datetime(1900, 1, 1):
            raise ValueError("Date of birth must be later than 01/01/1900")
        if dt.date() >= today:
            raise ValueError("Date of birth must be before today")
        age = today.year - dt.year - ((today.month, today.day) < (dt.month, dt.day))
        if age < 18:
            raise ValueError("You must be at least 18 years old")
        return v

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str | None) -> str | None:
        if v is None:
            return None
        v = v.strip()
        if not v:
            return None
        if not re.fullmatch(r"(0\d{9}|\+84\d{9})", v):
            raise ValueError("Phone must be 10 digits starting with 0 or +84 followed by 9 digits")
        return v

    @field_validator("agree_terms")
    @classmethod
    def must_agree(cls, v: bool) -> bool:
        if not v:
            raise ValueError("You must agree to the terms of service")
        return v


class UserLogin(BaseModel):
    username: str
    password: str
    remember_me: bool = False
    device_name: str | None = None


class UserResponse(BaseModel):
    id: UUID
    username: str
    email: str
    full_name: str
    email_verified: bool
    verified_student: bool
    role: str
    avatar_url: str | None

    model_config = ConfigDict(from_attributes=True)


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    user: UserResponse
    requires_email_verification: bool = False


class TokenRefresh(BaseModel):
    refresh_token: str
    device_name: str | None = None


class ForgotPassword(BaseModel):
    email: str | None = None
    phone: str | None = None


class ResetPassword(BaseModel):
    email: EmailStr
    code: str
    new_password: str
    confirm_password: str

    @field_validator("new_password")
    @classmethod
    def validate_password(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        if not re.search(r"[A-Z]", v):
            raise ValueError("Password must contain at least one uppercase letter")
        if not re.search(r"[a-z]", v):
            raise ValueError("Password must contain at least one lowercase letter")
        if not re.search(r"\d", v):
            raise ValueError("Password must contain at least one digit")
        if len(v.encode("utf-8")) > 72:
            raise ValueError("Password must be at most 72 bytes")
        return v

    @field_validator("confirm_password")
    @classmethod
    def passwords_match(cls, v: str, info) -> str:
        if "new_password" in info.data and v != info.data["new_password"]:
            raise ValueError("Passwords do not match")
        return v


class EmailVerifyRequest(BaseModel):
    email: EmailStr
    code: str = Field(pattern=r"^\d{6}$")


class ResendVerificationRequest(BaseModel):
    email: EmailStr | None = None


class GoogleLoginRequest(BaseModel):
    id_token: str
    device_name: str | None = None


class StudentVerificationRequest(BaseModel):
    full_name: str = Field(min_length=2, max_length=100)
    student_email: EmailStr | None = None
    university: str = Field(min_length=2, max_length=200)
    student_id: str = Field(min_length=3, max_length=50)
    academic_year: str = Field(min_length=1, max_length=20)
