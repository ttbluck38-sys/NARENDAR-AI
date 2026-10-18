# StudyPilot AI

**Adaptive learning platform — Android + FastAPI + PostgreSQL + AI-ready services.**

StudyPilot is designed as a real student product: personalized learning, curriculum navigation, quizzes, exams, revision, mistakes, notes, bookmarks, study tools, analytics, gamification and an AI tutor.

## Current architecture
- `android/` — Kotlin + Jetpack Compose Android client
- `backend/` — FastAPI API and learning logic
- `database/` — reserved for durable PostgreSQL migrations/seeds
- `.github/workflows/` — backend CI and Android build
- `docker-compose.yml` — local API + PostgreSQL stack

## Core feature set
Authentication, grade profiles, grades 1–10 curriculum, subjects, topics, search, progress tracking, accuracy analytics, study time, XP, levels, streaks, recommendations, AI tutor, explanation mode, quiz generation, difficulty selection, MCQs, explanations, notes, bookmarks, revision architecture, multiplication tables, calculator, dashboard, learning library, practice hub, flashcard-ready architecture, mock-exam-ready architecture, focus-study-ready architecture, notifications-ready architecture, profile settings, language/theme architecture, REST API, OpenAPI docs, Docker, PostgreSQL local stack, automated backend tests and Android APK CI.

## Secrets
Never commit API keys. Configure secrets only in the runtime/deployment environment. Minimum AI configuration is `OPENAI_API_KEY` and `AI_MODEL` if a remote AI provider is enabled. `DATABASE_URL` and `JWT_SECRET` also belong only in the runtime environment.

## Run backend
```bash
cd backend
python -m venv .venv
pip install -r requirements.txt
uvicorn app.main:app --reload
```

API docs: `http://127.0.0.1:8000/docs`

## Run stack
Copy `backend/.env.example` to `backend/.env`, then:
```bash
docker compose up --build
```

## Android
Open `android/` in Android Studio. The default emulator API URL is `http://10.0.2.2:8000`. For a physical phone, change `API_BASE_URL` in `android/app/build.gradle.kts` to the reachable backend address.

## Status
The repository now contains a real development baseline with working backend endpoints, a native Compose UI, tests, Docker configuration and CI. Production deployment, provider API keys and final device acceptance testing still need to be performed before a production release.
