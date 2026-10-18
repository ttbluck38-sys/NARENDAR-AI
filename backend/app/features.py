from fastapi import HTTPException
from pydantic import BaseModel, Field
from datetime import datetime, timezone
from .main import app, auth, PROGRESS, MISTAKES

class MistakeIn(BaseModel):
    question: str = Field(min_length=1, max_length=5000)
    answer: str = ""
    correct_answer: str = ""
    subject: str = "General"
    topic: str = "General"

class FlashcardIn(BaseModel):
    front: str
    back: str
    subject: str = "General"
    topic: str = "General"

class PlanIn(BaseModel):
    minutes_per_day: int = Field(default=30, ge=5, le=300)
    days: int = Field(default=7, ge=1, le=30)
    goal: str = "Improve weak topics"

class ConverterIn(BaseModel):
    value: float
    from_unit: str
    to_unit: str

@app.post('/api/v1/mistakes')
def add_mistake(data: MistakeIn, authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    item={**data.model_dump(),'id':datetime.now(timezone.utc).timestamp(),'reviewed':False}
    MISTAKES[u['email']].append(item)
    return item

@app.get('/api/v1/mistakes')
def mistakes(authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    return {'mistakes':MISTAKES[u['email']]}

@app.post('/api/v1/revision/queue')
def revision_queue(authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    weak=[]
    for key,v in PROGRESS[u['email']].items():
        if v['attempted'] and v['correct']/v['attempted'] < .8: weak.append({'topic':key,'reason':'below 80% accuracy'})
    return {'queue':weak + MISTAKES[u['email']][:10], 'algorithm':'weakness-first spaced review'}

@app.post('/api/v1/planner')
def planner(data: PlanIn, authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    p=PROGRESS[u['email']]
    topics=list(p.keys()) or ['Mathematics:Fractions','Mathematics:Percentages','English:Grammar','Science:Energy']
    schedule=[]
    for day in range(1,data.days+1):
        schedule.append({'day':day,'minutes':data.minutes_per_day,'focus':topics[(day-1)%len(topics)],'goal':data.goal})
    return {'student':u['name'],'plan':schedule}

@app.post('/api/v1/tools/converter')
def converter(data: ConverterIn):
    units={'m':1,'km':1000,'cm':.01,'mm':.001,'ft':.3048,'in':.0254,'mi':1609.344,'kg':1,'g':.001,'lb':.45359237,'min':60,'h':3600,'s':1}
    if data.from_unit not in units or data.to_unit not in units: raise HTTPException(400,'Unsupported unit')
    return {'value':data.value,'from':data.from_unit,'to':data.to_unit,'result':data.value*units[data.from_unit]/units[data.to_unit]}

@app.get('/api/v1/formulas')
def formulas(subject: str='Mathematics'):
    formulas={
      'Mathematics':[{'name':'Percentage','formula':'part / whole × 100'},{'name':'Rectangle area','formula':'length × width'},{'name':'Triangle area','formula':'1/2 × base × height'}],
      'Physics':[{'name':'Speed','formula':'distance / time'},{'name':'Force','formula':'mass × acceleration'},{'name':'Power','formula':'work / time'}],
      'Science':[{'name':'Density','formula':'mass / volume'}]
    }
    return {'subject':subject,'formulas':formulas.get(subject,formulas['Mathematics'])}

@app.get('/api/v1/achievements')
def achievements(authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    xp=u['xp']; p=PROGRESS[u['email']]; attempted=sum(v['attempted'] for v in p.values())
    all_items=[('first_steps', 'First Steps', attempted>=1),('quiz_50','50 Practice Questions',attempted>=50),('xp_100','100 XP',xp>=100),('xp_500','500 XP',xp>=500),('consistent','7 Day Streak',u['streak']>=7)]
    return {'achievements':[{'id':a,'title':t,'unlocked':ok} for a,t,ok in all_items]}

@app.get('/api/v1/stats/weekly')
def weekly_stats(authorization: str | None = None):
    u=auth(authorization.removeprefix('Bearer ') if authorization else '')
    p=PROGRESS[u['email']]
    total=sum(v['attempted'] for v in p.values()); correct=sum(v['correct'] for v in p.values()); minutes=sum(v['study_minutes'] for v in p.values())
    return {'days':[{'day':i,'minutes':minutes//7,'questions':total//7} for i in range(1,8)],'weekly_accuracy':round(correct/total*100,1) if total else 0}

@app.get('/api/v1/study/modes')
def study_modes():
    return {'modes':[{'id':'learn','title':'Learn','description':'Understand a concept with examples'},{'id':'practice','title':'Practice','description':'Targeted questions'},{'id':'revision','title':'Revision','description':'Review weak and forgotten material'},{'id':'exam','title':'Exam','description':'Timed assessment'},{'id':'focus','title':'Focus','description':'Distraction-free study timer'}]}
