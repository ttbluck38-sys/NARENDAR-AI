# StudyPilot AI

**From learning questions to an adaptive personal study system.**

StudyPilot AI is a native Android + FastAPI learning platform with persistent PostgreSQL data and an optional OpenAI-powered tutor/question generator. The app is designed around real student progress rather than a collection of static screens.

## What is implemented
- Native Kotlin + Jetpack Compose Android client
- Email/password registration and login with JWT access tokens
- Persistent users, progress, notes, bookmarks and mistakes
- Grades 1–10 curriculum, subjects, topics and search
- Dashboard with accuracy, XP, level, streak and mastery
- Mastery-based recommendations and revision queue
- Attempt recording and mistake capture
- AI Tutor with simple/deep/exam teaching modes
- AI quiz generation with deterministic fallback question bank
- Multiplication tables, safe arithmetic calculator and unit conversion
- Formula reference, planner and achievements APIs
- Weekly learning statistics
- Health/readiness endpoints for deployment monitoring
- Docker/local PostgreSQL support
- Render Blueprint for API + PostgreSQL
- Backend pytest CI and Android debug-APK CI
- Production API URL configurable through `STUDYPILOT_API_BASE_URL`

## Architecture
```text
Android app
   │ HTTPS + JWT
   ▼
FastAPI backend ─────► OpenAI API (optional)
   │
   ▼
PostgreSQL
```

## Environment variables
Never commit real secrets.

```text
DATABASE_URL=postgresql+psycopg://...
JWT_SECRET=long-random-secret
OPENAI_API_KEY=...
AI_MODEL=gpt-5-mini
APP_ENV=production
ACCESS_TOKEN_MINUTES=10080
```

`OPENAI_API_KEY` is optional: without it the tutor and quiz generator use safe built-in fallbacks. With it, the backend calls the OpenAI Responses API server-side; the key never belongs in the Android app.

## Local backend
```bash
cd backend
python -m venv .venv
# activate the environment
pip install -r requirements.txt
uvicorn app.main:app --reload
```

API documentation: `http://127.0.0.1:8000/docs`

For local SQLite, simply omit `DATABASE_URL`. For PostgreSQL, set `DATABASE_URL` before starting the API.

## Render deployment
The repository contains `render.yaml`. Connect the GitHub repository to Render and deploy the Blueprint. The Blueprint creates the FastAPI web service, connects `DATABASE_URL` to PostgreSQL, generates `JWT_SECRET`, and leaves `OPENAI_API_KEY` as a secret input.

After deployment, verify:
- `/health` returns `status: ok`
- `/readyz` returns `status: ready`
- `/docs` opens the FastAPI documentation

## Android build
The Android module uses Gradle 8.9 in CI and Java 17. For a local emulator the default backend is `http://10.0.2.2:8000`.

For a production build, set:
```text
STUDYPILOT_API_BASE_URL=https://YOUR-BACKEND-URL
```
then run:
```bash
cd android
gradle assembleDebug
# or
gradle bundleRelease
```

The GitHub Actions Android workflow automatically builds and uploads the debug APK after Android source changes.

## 30-day pilot
For the first 30 days, treat the release as a controlled pilot:
1. Keep the backend/database account secured.
2. Monitor `/health`, Render logs and database usage.
3. Test registration, login, dashboard, tutor, quiz, progress, notes and revision every few days.
4. Keep AI spending limits/budgets enabled on the AI provider.
5. Back up important PostgreSQL data before the free database period ends.
6. Do not publish the API key or signing credentials in GitHub.

## Release checklist
Before Play Store publication, complete production signing, privacy policy, app icon/screenshots, final device testing, backend monitoring and a production database plan. The 30-day pilot should come before broad public launch.
