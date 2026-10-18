from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    r=client.get('/health')
    assert r.status_code == 200
    assert r.json()['status'] == 'ok'

def test_curriculum():
    r=client.get('/api/v1/curriculum/8')
    assert r.status_code == 200
    assert 'Mathematics' in [x['name'] for x in r.json()['subjects']]

def test_tools_table():
    r=client.post('/api/v1/tools/table',json={'number':7,'upto':10})
    assert r.status_code == 200
    assert r.json()['rows'][-1]['answer'] == 70

def test_register_and_dashboard():
    r=client.post('/api/v1/auth/register',json={'name':'Test User','email':'test@example.com','password':'secret123','grade':8})
    assert r.status_code == 200
    token=r.json()['token']
    d=client.get('/api/v1/dashboard',headers={'authorization':f'Bearer {token}'})
    assert d.status_code == 200
    assert d.json()['level'] == 1
