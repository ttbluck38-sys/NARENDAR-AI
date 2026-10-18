from fastapi import FastAPI, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Literal
from datetime import datetime, timezone
import hashlib, secrets

app = FastAPI(title="StudyPilot AI API", version="1.1.0", description="Adaptive learning backend")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

USERS: dict[str, dict] = {}
SESSIONS: dict[str, str] = {}
PROGRESS: dict[str, dict[str, dict]] = {}
NOTES: dict[str, list[dict]] = {}
BOOKMARKS: dict[str, list[dict]] = {}
MISTAKES: dict[str, list[dict]] = {}

CURRICULUM = {
    "1": ["Mathematics", "English", "Science", "Computer Science"], "2": ["Mathematics", "English", "Science", "Computer Science"],
    "3": ["Mathematics", "English", "Science", "Computer Science"], "4": ["Mathematics", "English", "Science", "Computer Science"],
    "5": ["Mathematics", "English", "Science", "Computer Science"], "6": ["Mathematics", "English", "Science", "Computer Science"],
    "7": ["Mathematics", "English", "Science", "Computer Science"], "8": ["Mathematics", "English", "Science", "Computer Science"],
    "9": ["Mathematics", "English", "Physics", "Chemistry", "Biology", "Computer Science"], "10": ["Mathematics", "English", "Physics", "Chemistry", "Biology", "Computer Science"],
}
TOPICS = {
    "Mathematics": ["Numbers", "Fractions", "Decimals", "Percentages", "Ratio", "Algebra", "Geometry", "Statistics"],
    "English": ["Grammar", "Vocabulary", "Reading", "Writing", "Comprehension"],
    "Science": ["Matter", "Energy", "Forces", "Living Things", "Earth and Space"],
    "Physics": ["Motion", "Force", "Energy", "Waves", "Electricity"], "Chemistry": ["Atoms", "Elements", "Reactions", "Acids and Bases"],
    "Biology": ["Cells", "Genetics", "Human Body", "Ecology"], "Computer Science": ["Algorithms", "Programming", "Data", "Networks", "AI Basics"],
}

class RegisterIn(BaseModel):
    name: str = Field(min_length=2, max_length=80); email: str; password: str = Field(min_length=6, max_length=128); grade: int = Field(default=8, ge=1, le=12)
class LoginIn(BaseModel): email: str; password: str
class ProgressIn(BaseModel): subject: str; topic: str; correct: int = Field(default=0, ge=0); attempted: int = Field(default=0, ge=0); study_minutes: int = Field(default=0, ge=0)
class NoteIn(BaseModel): title: str = Field(min_length=1, max_length=120); body: str = Field(min_length=1, max_length=10000); subject: str = "General"
class BookmarkIn(BaseModel): item_id: str; title: str; item_type: str = "topic"
class TutorIn(BaseModel): question: str = Field(min_length=1, max_length=5000); subject: str = "General"; level: str = "simple"
class QuizIn(BaseModel): subject: str; topic: str; count: int = Field(default=10, ge=1, le=50); difficulty: Literal["easy", "medium", "hard"] = "medium"
class TableIn(BaseModel): number: int = Field(ge=1, le=1000); upto: int = Field(default=10, ge=1, le=50)
class CalcIn(BaseModel): expression: str = Field(min_length=1, max_length=200)

def token_for(_: str) -> str: return secrets.token_urlsafe(32)
def public_user(u): return {k: v for k, v in u.items() if k != "password"}
def auth(token: str | None = None):
    email = SESSIONS.get(token or "")
    if not email or email not in USERS: raise HTTPException(401, "Invalid or expired session")
    return USERS[email]
def auth_header(authorization: str | None): return authorization.removeprefix("Bearer ") if authorization else ""
def hash_password(password: str) -> str:
    salt = secrets.token_hex(16); return salt + ":" + hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 120000).hex()
def verify_password(password: str, stored: str) -> bool:
    salt, digest = stored.split(":", 1); return secrets.compare_digest(digest, hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 120000).hex())

@app.get("/health")
def health(): return {"status": "ok", "service": "studypilot-api", "version": app.version, "time": datetime.now(timezone.utc).isoformat()}

@app.post("/api/v1/auth/register")
def register(data: RegisterIn):
    email = data.email.lower().strip()
    if email in USERS: raise HTTPException(409, "Account already exists")
    USERS[email] = {"name": data.name.strip(), "email": email, "grade": data.grade, "password": hash_password(data.password), "xp": 0, "streak": 0, "created_at": datetime.now(timezone.utc).isoformat()}
    PROGRESS[email], NOTES[email], BOOKMARKS[email], MISTAKES[email] = {}, [], [], []
    token = token_for(email); SESSIONS[token] = email; return {"token": token, "user": public_user(USERS[email])}

@app.post("/api/v1/auth/login")
def login(data: LoginIn):
    email = data.email.lower().strip(); user = USERS.get(email)
    if not user or not verify_password(data.password, user["password"]): raise HTTPException(401, "Invalid email or password")
    token = token_for(email); SESSIONS[token] = email; return {"token": token, "user": public_user(user)}

@app.post("/api/v1/auth/logout")
def logout(authorization: str | None = Header(default=None)):
    token = auth_header(authorization); SESSIONS.pop(token, None); return {"ok": True}

@app.get("/api/v1/me")
def me(authorization: str | None = Header(default=None)): return public_user(auth(auth_header(authorization)))

@app.get("/api/v1/curriculum/{grade}")
def curriculum(grade: int):
    subjects = CURRICULUM.get(str(grade), CURRICULUM["8"]); return {"grade": grade, "subjects": [{"name": s, "topics": TOPICS.get(s, ["Introduction", "Practice", "Review"])} for s in subjects]}

@app.get("/api/v1/search")
def search(q: str, grade: int = 8):
    ql = q.lower().strip(); results = []
    for subject in CURRICULUM.get(str(grade), CURRICULUM["8"]):
        for topic in TOPICS.get(subject, []):
            if ql in subject.lower() or ql in topic.lower(): results.append({"subject": subject, "topic": topic, "type": "topic"})
    return {"query": q, "results": results[:30]}

@app.post("/api/v1/progress")
def progress(data: ProgressIn, authorization: str | None = Header(default=None)):
    u = auth(auth_header(authorization)); email = u["email"]; key = f"{data.subject}:{data.topic}"
    old = PROGRESS[email].get(key, {"attempted": 0, "correct": 0, "study_minutes": 0}); old["attempted"] += data.attempted; old["correct"] += min(data.correct, data.attempted); old["study_minutes"] += data.study_minutes; PROGRESS[email][key] = old
    u["xp"] += max(0, data.correct * 5 + data.study_minutes); u["streak"] = max(1, u["streak"])
    return {"key": key, "progress": old, "user": public_user(u)}

@app.get("/api/v1/progress")
def get_progress(authorization: str | None = Header(default=None)):
    u = auth(auth_header(authorization)); p = PROGRESS[u["email"]]; attempted = sum(x["attempted"] for x in p.values()); correct = sum(x["correct"] for x in p.values()); minutes = sum(x["study_minutes"] for x in p.values())
    return {"topics": p, "summary": {"attempted": attempted, "correct": correct, "accuracy": round(correct / attempted * 100, 1) if attempted else 0, "study_minutes": minutes, "xp": u["xp"], "streak": u["streak"]}}

@app.get("/api/v1/recommendations")
def recommendations(authorization: str | None = Header(default=None)):
    u = auth(auth_header(authorization)); weak = []
    for key, val in PROGRESS[u["email"]].items():
        acc = val["correct"] / val["attempted"] if val["attempted"] else 0
        if val["attempted"] >= 3 and acc < .7: weak.append((1 - acc, key))
    weak.sort(reverse=True); items = [{"reason": "weak topic", "topic": k, "priority": round(score, 2)} for score, k in weak[:10]]
    return {"recommendations": items or [{"reason": "build foundations", "topic": "Fractions", "priority": .5}, {"reason": "daily practice", "topic": "Percentages", "priority": .4}]}

@app.post("/api/v1/notes")
def add_note(data: NoteIn, authorization: str | None = Header(default=None)):
    u = auth(auth_header(authorization)); item = {"id": secrets.token_hex(8), **data.model_dump(), "created_at": datetime.now(timezone.utc).isoformat()}; NOTES[u["email"]].append(item); return item
@app.get("/api/v1/notes")
def notes(authorization: str | None = Header(default=None)): return {"notes": NOTES[auth(auth_header(authorization))["email"]]}
@app.post("/api/v1/bookmarks")
def add_bookmark(data: BookmarkIn, authorization: str | None = Header(default=None)):
    arr = BOOKMARKS[auth(auth_header(authorization))["email"]]
    if not any(x["item_id"] == data.item_id for x in arr): arr.append(data.model_dump())
    return {"bookmarks": arr}
@app.get("/api/v1/bookmarks")
def bookmarks(authorization: str | None = Header(default=None)): return {"bookmarks": BOOKMARKS[auth(auth_header(authorization))["email"]]}

@app.post("/api/v1/tools/table")
def table(data: TableIn): return {"number": data.number, "rows": [{"multiplier": i, "answer": data.number * i, "text": f"{data.number} × {i} = {data.number*i}"} for i in range(1, data.upto + 1)]}
@app.post("/api/v1/tools/calculator")
def calculator(data: CalcIn):
    allowed = set("0123456789+-*/().% ")
    if any(c not in allowed for c in data.expression): raise HTTPException(400, "Only basic arithmetic is supported")
    try: result = eval(data.expression, {"__builtins__": {}}, {})
    except Exception: raise HTTPException(400, "Invalid expression")
    return {"expression": data.expression, "result": result}

@app.post("/api/v1/ai/tutor")
def tutor(data: TutorIn):
    q, lower = data.question.strip(), data.question.lower()
    if "fraction" in lower: answer = "Fractions represent parts of a whole. Find a common denominator, combine numerators, then simplify."
    elif "percent" in lower: answer = "Percent means per hundred. x% of y = (x ÷ 100) × y."
    elif "photosynthesis" in lower: answer = "Photosynthesis uses light energy to convert water and carbon dioxide into glucose and oxygen."
    elif "gravity" in lower: answer = "Gravity is the attractive force between masses. Near Earth, acceleration is about 9.8 m/s²."
    else: answer = "Break the problem into: given information → required answer → rule/formula → steps → final check."
    return {"question": q, "answer": answer, "mode": data.level, "subject": data.subject, "next_steps": ["Try a worked example", "Practice one similar question", "Explain your answer in your own words"]}

@app.post("/api/v1/quizzes/generate")
def quiz(data: QuizIn):
    seed = [{"q": "What is 25% of 80?", "options": ["10", "20", "25", "40"], "answer": 1, "explanation": "25% = 1/4, and 80/4 = 20."}, {"q": "Which topic is being practiced?", "options": [data.topic, "History", "Art", "Music"], "answer": 0, "explanation": "The quiz was generated for the selected topic."}, {"q": "Which number is prime?", "options": ["21", "27", "29", "35"], "answer": 2, "explanation": "29 has no positive divisors other than 1 and itself."}]
    return {"subject": data.subject, "topic": data.topic, "difficulty": data.difficulty, "questions": [seed[i % len(seed)] for i in range(data.count)]}

@app.get("/api/v1/dashboard")
def dashboard(authorization: str | None = Header(default=None)):
    u = auth(auth_header(authorization)); p = PROGRESS[u["email"]]; attempts = sum(v["attempted"] for v in p.values()); correct = sum(v["correct"] for v in p.values()); minutes = sum(v["study_minutes"] for v in p.values()); level = 1 + u["xp"] // 100
    return {"user": public_user(u), "level": level, "xp_to_next": 100 - (u["xp"] % 100), "accuracy": round(correct / attempts * 100, 1) if attempts else 0, "study_minutes": minutes, "topics_mastered": sum(1 for v in p.values() if v["attempted"] and v["correct"] / v["attempted"] >= .8), "quick_actions": ["Tutor", "Quiz", "Revision", "Tables", "Calculator", "Notes"]}

# Registers the extended feature endpoints without creating a circular import at module load time.
from . import features  # noqa: F401,E402
