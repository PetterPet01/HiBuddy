# HiBuddy

HiBuddy is an Android and FastAPI application for matching students to project
teams. The upgraded application includes verified authentication, role-scoped
skills, explainable project matching, contextual swipes, chat, project/task
workflows, moderation, media uploads, push notifications, and admin operations.

## Local Setup

Use Python 3.12 (3.10-3.12 are supported). The pinned PyTorch release is not
available for Python 3.14.

```bash
cd backend
cp .env.example .env
docker compose up -d
python3.12 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/alembic upgrade head
.venv/bin/python seed_data.py
.venv/bin/uvicorn app.main:app --reload
```

Install `backend/requirements-ml.txt` instead only when local embeddings and
Milvus similarity search are enabled.

The API is available at `http://localhost:8000` and OpenAPI documentation at
`http://localhost:8000/docs`.

For Android, set `BASE_URL` in [app/build.gradle.kts](app/build.gradle.kts) for
the emulator or device, add a Firebase `google-services.json` when push
notifications are required, then run:

```bash
./gradlew :app:assembleDebug
```

## Demo Scenarios

All seeded accounts use password `HiBuddyDemo!2026`.

- `admin@hibuddy.local`: admin dashboards, student review, reports, flagged projects
- `minh@example.com`: project owner with approved and manual-review projects
- `thu@example.com`: verified contributor with design-role skills
- `lan@example.com`: pending student verification with evidence
- `pending@example.com`: unverified-email access restrictions
- `banned@example.com`: banned-account and discovery filtering

The seed also creates mutual matches, chats, tasks in each lifecycle state,
super likes, a pending report, a block, role-specific skill assignments, and
structured project role requirements.

## Verification

```bash
cd backend
.venv/bin/pytest
python3 -m compileall app alembic seed_data.py
cd ..
./gradlew testDebugUnitTest :app:assembleDebug
```

Production deployments must set `ENVIRONMENT=production`, disable `DEBUG`, use
a strong `SECRET_KEY`, configure non-wildcard CORS, SMTP, Google OAuth, Firebase
credentials, persistent PostgreSQL/Redis/MinIO services, and run migrations
before starting the API.
