from __future__ import annotations

import ast, hashlib, json, math, os, secrets
from datetime import datetime, timedelta, timezone
from typing import Literal

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from jose import JWTError, jwt
from sqlalchemy import select

from .database import SessionLocal, init_db, User, ProgressRecord, Note, Bookmark, Mistake

APP_ENV = os.getenv("APP_ENV", "development")
JWT_SECRET = os.getenv("JWT_SECRET", "change-this-in-production")
JWT_ALGORITHM = "HS256"
ACCESS_MINUTES = int(os.getenv("ACCESS_TOKEN_MINUTES", "10080"))
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
AI_MODEL = os.getenv("AI_MODEL", "gpt-5-mini")

app = FastAPI(title="StudyPilot AI API", version="2.0.0", description="Adaptive learning backend")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True, allow_methods=["*"], allow_headers=["*"])

CURRICULUM = {str(g): ["Mathematics", "English", "Science", "Computer Science"] for g in range(1, 9)}
CURRICULUM.update({str(g): ["Mathematics", "English", "Physics", "Chemistry", "Biology", "Computer Science"] for g in (9, 10)})
TOPICS = {
    "Mathematics": ["Numbers", "Fractions", "Decimals", "Percentages", "Ratio", "Algebra", "Geometry", "Statistics"],
    "English": ["Grammar", "Vocabulary", "Reading", "Writing", "Comprehension"],
    "Science": ["Matter", "Energy", "Forces", "Living Things", "Earth and Space"],
    "Physics": ["Motion", "Force", "Energy", "Waves", "Electricity"],
    "Chemistry": ["Atoms", "Elements", "Reactions", "Acids and Bases"],
    "Biology": ["Cells", "Genetics", "Human Body", "Ecology"],
    "Computer Science": ["Algorithms", "Programming", "Data", "Networks", "AI Basics"],
}

class RegisterIn(BaseModel):
    name: str = Field(min_length=2, max_length=80)
    email: str
    password: str = Field(min_length=6, max_length=128)
    grade: int = Field(default=8, ge=1, le=10)
class LoginIn(BaseModel): email: str; password: str
class ProgressIn(BaseModel):
    subject: str; topic: str; correct: int = Field(default=0, ge=0); attempted: int = Field(default=0, ge=0); study_minutes: int = Field(default=0, ge=0)
class NoteIn(BaseModel): title: str = Field(min_length=1, max_length=120); body: str = Field(min_length=1, max_length=10000); subject: str = "General"
class BookmarkIn(BaseModel): item_id: str = Field(max_length=200); title: str = Field(max_length=200); item_type: str = "topic"
class TutorIn(BaseModel): question: str = Field(min_length=1, max_length=5000); subject: str = "General"; level: Literal["simple", "deep", "exam"] = "simple"
class QuizIn(BaseModel): subject: str; topic: str; count: int = Field(default=10, ge=1, le=30); difficulty: Literal["easy", "medium", "hard"] = "medium"
class AttemptIn(BaseModel): subject: str; topic: str; correct: int = Field(ge=0); attempted: int = Field(ge=0); study_minutes: int = Field(default=0, ge=0); mistakes: list[dict] = Field(default_factory=list)
class TableIn(BaseModel): number: int = Field(ge=1, le=1000); upto: int = Field(default=10, ge=1, le=50)
class CalcIn(BaseModel): expression: str = Field(min_length=1, max_length=200)
class PlanIn(BaseModel): minutes_per_day: int = Field(default=30, ge=5, le=300); days: int = Field(default=7, ge=1, le=30); goal: str = "Improve weak topics"
class ConverterIn(BaseModel): value: float; from_unit: str; to_unit: str

@app.on_event("startup")
def startup(): init_db()

def hash_password(password: str) -> str:
    salt = secrets.token_hex(16)
    return salt + ":" + hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 150000).hex()
def verify_password(password: str, stored: str) -> bool:
    try:
        salt, digest = stored.split(":", 1)
        return secrets.compare_digest(digest, hashlib.pbkdf2_hmac("sha256", password.encode(), salt.encode(), 150000).hex())
    except ValueError: return False

def token_for(user_id: int) -> str:
    return jwt.encode({"sub": str(user_id), "exp": datetime.now(timezone.utc) + timedelta(minutes=ACCESS_MINUTES)}, JWT_SECRET, algorithm=JWT_ALGORITHM)
def auth(authorization: str | None) -> User:
    token = (authorization or "").removeprefix("Bearer ").strip()
    if not token: raise HTTPException(401, "Authentication required")
    try: uid = int(jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM]).get("sub", "0"))
    except (JWTError, ValueError): raise HTTPException(401, "Invalid or expired session")
    with SessionLocal() as db:
        user = db.get(User, uid)
        if not user: raise HTTPException(401, "Account not found")
        db.expunge(user); return user

def public_user(u: User): return {"id": u.id, "name": u.name, "email": u.email, "grade": u.grade, "xp": u.xp, "streak": u.streak, "created_at": u.created_at.isoformat()}

def progress_summary(db, uid: int):
    rows = db.scalars(select(ProgressRecord).where(ProgressRecord.user_id == uid)).all()
    attempted, correct, minutes = sum(x.attempted for x in rows), sum(x.correct for x in rows), sum(x.study_minutes for x in rows)
    return rows, {"attempted": attempted, "correct": correct, "accuracy": round(correct / attempted * 100, 1) if attempted else 0, "study_minutes": minutes}

@app.get("/health")
def health(): return {"status":"ok", "service":"studypilot-api", "version":app.version, "database":"configured"}
@app.get("/readyz")
def readyz():
    try:
        with SessionLocal() as db: db.execute(select(User).limit(1))
        return {"status":"ready"}
    except Exception as exc: raise HTTPException(503, f"database unavailable: {type(exc).__name__}")

@app.post("/api/v1/auth/register")
def register(data: RegisterIn):
    email = data.email.lower().strip()
    with SessionLocal() as db:
        if db.scalar(select(User).where(User.email == email)): raise HTTPException(409, "Account already exists")
        user = User(name=data.name.strip(), email=email, password_hash=hash_password(data.password), grade=data.grade)
        db.add(user); db.commit(); db.refresh(user)
        return {"token":token_for(user.id), "user":public_user(user)}
@app.post("/api/v1/auth/login")
def login(data: LoginIn):
    with SessionLocal() as db:
        user = db.scalar(select(User).where(User.email == data.email.lower().strip()))
        if not user or not verify_password(data.password, user.password_hash): raise HTTPException(401, "Invalid email or password")
        return {"token":token_for(user.id), "user":public_user(user)}
@app.post("/api/v1/auth/logout")
def logout(): return {"ok":True, "message":"JWT access tokens expire automatically"}
@app.get("/api/v1/me")
def me(authorization: str | None = Header(default=None)): return public_user(auth(authorization))

@app.get("/api/v1/curriculum/{grade}")
def curriculum(grade: int):
    subjects = CURRICULUM.get(str(grade), CURRICULUM["8"])
    return {"grade":grade, "subjects":[{"name":s,"topics":TOPICS.get(s,[]) } for s in subjects]}
@app.get("/api/v1/search")
def search(q: str, grade: int = 8):
    ql=q.lower().strip(); out=[]
    for s in CURRICULUM.get(str(grade), CURRICULUM["8"]):
        for t in TOPICS.get(s,[]):
            if ql in s.lower() or ql in t.lower(): out.append({"subject":s,"topic":t,"type":"topic"})
    return {"query":q,"results":out[:30]}

@app.post("/api/v1/progress")
def progress(data: ProgressIn, authorization: str | None = Header(default=None)):
    return record_attempt(data, authorization)
@app.post("/api/v1/attempts")
def attempts(data: AttemptIn, authorization: str | None = Header(default=None)):
    result = record_attempt(ProgressIn(subject=data.subject, topic=data.topic, correct=data.correct, attempted=data.attempted, study_minutes=data.study_minutes), authorization)
    u=auth(authorization)
    with SessionLocal() as db:
        for m in data.mistakes[:20]: db.add(Mistake(user_id=u.id, question=str(m.get("question","")), answer=str(m.get("answer","")), correct_answer=str(m.get("correct_answer","")), subject=data.subject, topic=data.topic))
        db.commit()
    return result

def record_attempt(data: ProgressIn, authorization: str | None):
    u=auth(authorization)
    if data.correct > data.attempted: raise HTTPException(400,"correct cannot exceed attempted")
    with SessionLocal() as db:
        row=db.scalar(select(ProgressRecord).where(ProgressRecord.user_id==u.id,ProgressRecord.subject==data.subject,ProgressRecord.topic==data.topic))
        if not row: row=ProgressRecord(user_id=u.id,subject=data.subject,topic=data.topic); db.add(row)
        row.attempted += data.attempted; row.correct += data.correct; row.study_minutes += data.study_minutes
        row.mastery = round(min(1.0, row.correct / row.attempted) if row.attempted else 0.0, 3); row.last_studied=datetime.now(timezone.utc)
        user=db.get(User,u.id); user.xp += data.correct*5 + data.study_minutes; user.streak=max(1,user.streak)
        db.commit(); db.refresh(row); db.refresh(user)
        return {"key":f"{data.subject}:{data.topic}","progress":{"attempted":row.attempted,"correct":row.correct,"mastery":row.mastery,"study_minutes":row.study_minutes},"user":public_user(user)}

@app.get("/api/v1/progress")
def get_progress(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        rows,summary=progress_summary(db,u.id); summary.update({"xp":u.xp,"streak":u.streak})
        return {"topics":{f"{r.subject}:{r.topic}":{"attempted":r.attempted,"correct":r.correct,"mastery":r.mastery,"study_minutes":r.study_minutes} for r in rows},"summary":summary}
@app.get("/api/v1/dashboard")
def dashboard(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        rows,s=progress_summary(db,u.id)
        return {"user":public_user(u),"level":1+u.xp//100,"xp_to_next":100-(u.xp%100),"accuracy":s["accuracy"],"study_minutes":s["study_minutes"],"topics_mastered":sum(1 for r in rows if r.mastery>=.8 and r.attempted>=3),"quick_actions":["Tutor","Quiz","Revision","Tables","Calculator","Notes"]}

@app.get("/api/v1/recommendations")
def recommendations(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: rows,_=progress_summary(db,u.id)
    weak=sorted([r for r in rows if r.attempted>=3], key=lambda r:r.mastery)
    items=[{"reason":"weak topic","subject":r.subject,"topic":r.topic,"priority":round(1-r.mastery,2)} for r in weak[:10]]
    return {"recommendations":items or [{"reason":"build foundations","subject":"Mathematics","topic":"Fractions","priority":.5},{"reason":"daily practice","subject":"Mathematics","topic":"Percentages","priority":.4}]}

@app.post("/api/v1/notes")
def add_note(data: NoteIn, authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        n=Note(user_id=u.id,title=data.title,body=data.body,subject=data.subject); db.add(n); db.commit(); db.refresh(n)
        return {"id":n.id,"title":n.title,"body":n.body,"subject":n.subject,"created_at":n.created_at.isoformat()}
@app.get("/api/v1/notes")
def notes(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        return {"notes":[{"id":n.id,"title":n.title,"body":n.body,"subject":n.subject,"created_at":n.created_at.isoformat()} for n in db.scalars(select(Note).where(Note.user_id==u.id).order_by(Note.id.desc())).all()]}
@app.post("/api/v1/bookmarks")
def add_bookmark(data: BookmarkIn, authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        old=db.scalar(select(Bookmark).where(Bookmark.user_id==u.id,Bookmark.item_id==data.item_id))
        if not old: db.add(Bookmark(user_id=u.id,**data.model_dump())); db.commit()
        return bookmarks(authorization)
@app.get("/api/v1/bookmarks")
def bookmarks(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: return {"bookmarks":[{"id":b.id,"item_id":b.item_id,"title":b.title,"item_type":b.item_type} for b in db.scalars(select(Bookmark).where(Bookmark.user_id==u.id)).all()]}

@app.post("/api/v1/mistakes")
def add_mistake(data: dict, authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        m=Mistake(user_id=u.id,question=str(data.get("question","")),answer=str(data.get("answer","")),correct_answer=str(data.get("correct_answer","")),subject=str(data.get("subject","General")),topic=str(data.get("topic","General"))); db.add(m); db.commit(); db.refresh(m)
        return {"id":m.id,"question":m.question,"subject":m.subject,"topic":m.topic,"reviewed":m.reviewed}
@app.get("/api/v1/mistakes")
def mistakes(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: return {"mistakes":[m.__dict__ | {} for m in db.scalars(select(Mistake).where(Mistake.user_id==u.id).order_by(Mistake.id.desc())).all()]}
@app.post("/api/v1/revision/queue")
def revision_queue(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db:
        rows,_=progress_summary(db,u.id); ms=db.scalars(select(Mistake).where(Mistake.user_id==u.id,Mistake.reviewed==False).limit(20)).all()
        return {"queue":[{"type":"topic","subject":r.subject,"topic":r.topic,"mastery":r.mastery} for r in sorted(rows,key=lambda x:x.mastery) if r.mastery<.8] + [{"type":"mistake","id":m.id,"subject":m.subject,"topic":m.topic,"question":m.question} for m in ms],"algorithm":"mastery-first spaced review"}

@app.post("/api/v1/planner")
def planner(data: PlanIn, authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: rows,_=progress_summary(db,u.id)
    topics=[f"{r.subject}:{r.topic}" for r in sorted(rows,key=lambda x:x.mastery)] or ["Mathematics:Fractions","Mathematics:Percentages","English:Grammar","Science:Energy"]
    return {"student":u.name,"plan":[{"day":d,"minutes":data.minutes_per_day,"focus":topics[(d-1)%len(topics)],"goal":data.goal} for d in range(1,data.days+1)]}

@app.post("/api/v1/tools/table")
def table(data: TableIn): return {"number":data.number,"rows":[{"multiplier":i,"answer":data.number*i,"text":f"{data.number} × {i} = {data.number*i}"} for i in range(1,data.upto+1)]}

class SafeCalculator(ast.NodeVisitor):
    allowed=(ast.Expression,ast.BinOp,ast.UnaryOp,ast.Constant,ast.Add,ast.Sub,ast.Mult,ast.Div,ast.Mod,ast.Pow,ast.USub,ast.UAdd)
    def visit(self,node):
        if not isinstance(node,self.allowed): raise ValueError("unsupported")
        return super().visit(node)
@app.post("/api/v1/tools/calculator")
def calculator(data: CalcIn):
    try:
        tree=ast.parse(data.expression,mode="eval"); SafeCalculator().visit(tree); value=eval(compile(tree,"<calc>","eval"),{"__builtins__":{}}, {})
        if not isinstance(value,(int,float)) or not math.isfinite(float(value)): raise ValueError()
        return {"expression":data.expression,"result":value}
    except Exception: raise HTTPException(400,"Invalid arithmetic expression")
@app.post("/api/v1/tools/converter")
def converter(data: ConverterIn):
    units={"m":1,"km":1000,"cm":.01,"mm":.001,"ft":.3048,"in":.0254,"mi":1609.344,"kg":1,"g":.001,"lb":.45359237,"min":60,"h":3600,"s":1}
    if data.from_unit not in units or data.to_unit not in units: raise HTTPException(400,"Unsupported unit")
    return {"value":data.value,"from":data.from_unit,"to":data.to_unit,"result":data.value*units[data.from_unit]/units[data.to_unit]}
@app.get("/api/v1/formulas")
def formulas(subject: str="Mathematics"):
    data={"Mathematics":[("Percentage","part / whole × 100"),("Rectangle area","length × width"),("Triangle area","1/2 × base × height")],"Physics":[("Speed","distance / time"),("Force","mass × acceleration"),("Power","work / time")],"Science":[("Density","mass / volume")]}
    return {"subject":subject,"formulas":[{"name":n,"formula":f} for n,f in data.get(subject,data["Mathematics"])]}

QUESTION_BANK=[
 {"subject":"Mathematics","topic":"Percentages","difficulty":"easy","q":"What is 25% of 80?","options":["10","20","25","40"],"answer":1,"explanation":"25% is one quarter; 80 ÷ 4 = 20."},
 {"subject":"Mathematics","topic":"Numbers","difficulty":"easy","q":"Which number is prime?","options":["21","27","29","35"],"answer":2,"explanation":"29 has no factors other than 1 and 29."},
 {"subject":"Mathematics","topic":"Fractions","difficulty":"medium","q":"What is 1/2 + 1/4?","options":["1/6","2/6","3/4","1"],"answer":2,"explanation":"Convert 1/2 to 2/4, then add to get 3/4."},
 {"subject":"Science","topic":"Energy","difficulty":"easy","q":"Which form of energy comes from the Sun?","options":["Solar energy","Sound only","Chemical waste","None"],"answer":0,"explanation":"The Sun provides radiant energy that reaches Earth as light and heat."},
 {"subject":"Science","topic":"Forces","difficulty":"medium","q":"What is a force?","options":["A push or pull","Only heat","A color","A unit of time"],"answer":0,"explanation":"A force is an interaction that can change an object's motion."},
 {"subject":"English","topic":"Grammar","difficulty":"easy","q":"Which word is a verb?","options":["Run","Blue","Quickly","Table"],"answer":0,"explanation":"Run names an action, so it is a verb."},
 {"subject":"Computer Science","topic":"Algorithms","difficulty":"easy","q":"What is an algorithm?","options":["A step-by-step procedure","A battery","A screen","A cable"],"answer":0,"explanation":"An algorithm is a finite sequence of steps for solving a problem."},
]

def fallback_quiz(data: QuizIn):
    pool=[q for q in QUESTION_BANK if q["subject"]==data.subject and q["topic"]==data.topic and q["difficulty"]==data.difficulty] or [q for q in QUESTION_BANK if q["subject"]==data.subject] or QUESTION_BANK
    return [pool[i%len(pool)] for i in range(data.count)]

async def ai_text(prompt: str, system: str) -> str | None:
    if not OPENAI_API_KEY: return None
    payload={"model":AI_MODEL,"input":[{"role":"system","content":system},{"role":"user","content":prompt}],"max_output_tokens":900}
    headers={"Authorization":f"Bearer {OPENAI_API_KEY}","Content-Type":"application/json"}
    try:
        async with httpx.AsyncClient(timeout=45) as client:
            r=await client.post("https://api.openai.com/v1/responses",headers=headers,json=payload); r.raise_for_status(); data=r.json()
        if data.get("output_text"): return data["output_text"]
        for item in data.get("output",[]):
            for c in item.get("content",[]):
                if c.get("type")=="output_text": return c.get("text")
    except Exception: return None
    return None

@app.post("/api/v1/ai/tutor")
async def tutor(data: TutorIn, authorization: str | None = Header(default=None)):
    u=auth(authorization)
    system=f"You are StudyPilot AI, a patient tutor for grade {u.grade}. Teach clearly, never just give an unexplained answer. Level={data.level}. Subject={data.subject}. Use numbered steps, a short example, and one practice question."
    text=await ai_text(data.question,system)
    if text: return {"question":data.question,"answer":text,"mode":data.level,"subject":data.subject,"ai":True}
    return {"question":data.question,"answer":"Break the problem into: given information → required answer → rule/formula → steps → final check. Add your question to the practice set after this explanation.","mode":data.level,"subject":data.subject,"ai":False}

@app.post("/api/v1/quizzes/generate")
async def quiz(data: QuizIn, authorization: str | None = Header(default=None)):
    auth(authorization)
    prompt=f"Create exactly {data.count} multiple-choice questions for grade 8, subject {data.subject}, topic {data.topic}, difficulty {data.difficulty}. Return ONLY valid JSON: {{\"questions\":[{{\"q\":string,\"options\":[string,string,string,string],\"answer\":0,\"explanation\":string}}]}}. answer is zero-based. No markdown."
    text=await ai_text(prompt,"You are an educational question writer. Questions must be factually correct, age appropriate and unambiguous.")
    if text:
        try:
            clean=text.strip().removeprefix("```json").removesuffix("```").strip(); questions=json.loads(clean)["questions"]
            if len(questions)>=data.count: return {"subject":data.subject,"topic":data.topic,"difficulty":data.difficulty,"ai":True,"questions":questions[:data.count]}
        except Exception: pass
    return {"subject":data.subject,"topic":data.topic,"difficulty":data.difficulty,"ai":False,"questions":fallback_quiz(data)}

@app.get("/api/v1/achievements")
def achievements(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: _,s=progress_summary(db,u.id)
    items=[("first_steps","First Steps",s["attempted"]>=1),("quiz_50","50 Questions",s["attempted"]>=50),("xp_100","100 XP",u.xp>=100),("xp_500","500 XP",u.xp>=500),("streak_7","7 Day Streak",u.streak>=7)]
    return {"achievements":[{"id":i,"title":t,"unlocked":ok} for i,t,ok in items]}
@app.get("/api/v1/stats/weekly")
def weekly_stats(authorization: str | None = Header(default=None)):
    u=auth(authorization)
    with SessionLocal() as db: _,s=progress_summary(db,u.id)
    return {"days":[{"day":i,"minutes":s["study_minutes"]//7,"questions":s["attempted"]//7} for i in range(1,8)],"weekly_accuracy":s["accuracy"]}
@app.get("/api/v1/study/modes")
def study_modes(): return {"modes":[{"id":"learn","title":"Learn","description":"Understand concepts"},{"id":"practice","title":"Practice","description":"Targeted questions"},{"id":"revision","title":"Revision","description":"Review weak material"},{"id":"exam","title":"Exam","description":"Timed assessment"},{"id":"focus","title":"Focus","description":"Distraction-free timer"}]}
