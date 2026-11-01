const { onRequest } = require('firebase-functions/v2/https');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

const OPENROUTER_URL = 'https://openrouter.ai/api/v1/chat/completions';
const MODEL = 'openrouter/free';

function json(res, status, body) {
  res.status(status).set('Content-Type', 'application/json').send(JSON.stringify(body));
}

function normalize(text) {
  return String(text || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

async function allowRequest(req) {
  const ip = String(req.headers['x-forwarded-for'] || req.ip || 'unknown').split(',')[0].trim();
  const bucket = Math.floor(Date.now() / 3600000);
  const id = `${ip.replace(/[^a-zA-Z0-9_.-]/g, '_')}_${bucket}`;
  const ref = db.collection('ai_rate_limits').doc(id);
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const count = snap.exists ? Number(snap.data().count || 0) : 0;
    if (count >= 30) return false;
    tx.set(ref, { count: count + 1, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    return true;
  });
}

async function generateMcqs(req, res) {
  if (req.method !== 'POST') return json(res, 405, { error: 'POST required.' });

  const clientMarker = req.headers.authorization || '';
  if (clientMarker !== 'Bearer firebase-client') {
    return json(res, 401, { error: 'StudyPilot client authorization required.' });
  }

  const allowed = await allowRequest(req).catch((err) => {
    logger.error('Rate-limit check failed', err);
    return true;
  });
  if (!allowed) return json(res, 429, { error: 'Hourly AI request limit reached. Please try again later.' });

  const apiKey = process.env.OPENROUTER_API_KEY;
  if (!apiKey) return json(res, 500, { error: 'OpenRouter backend key is not configured.' });

  const body = req.body || {};
  const grade = Math.max(1, Math.min(12, Number(body.grade || 8)));
  const subject = String(body.subject || 'Mathematics').slice(0, 80);
  const difficulty = String(body.difficulty || 'Mixed').slice(0, 30);
  const language = String(body.language || 'English').slice(0, 40);
  const studentName = String(body.studentName || 'Student').slice(0, 80);
  const learningLevel = String(body.learningLevel || '').slice(0, 200);
  const count = Math.max(1, Math.min(10, Number(body.count || 10)));
  const excluded = Array.isArray(body.excludeQuestions) ? body.excludeQuestions.map(normalize).filter(Boolean).slice(0, 100) : [];

  const prompt = `You are StudyPilot AI, a careful school-learning assistant. Generate exactly ${count} unique multiple-choice questions.
Student: ${studentName}
Class: ${grade}
Subject: ${subject}
Difficulty: ${difficulty}
Language: ${language}
Learning context: ${learningLevel}

Rules:
- Return ONLY valid JSON.
- Schema: {"questions":[{"question":"...","options":["A","B","C","D"],"answerIndex":0,"explanation":"...","topic":"...","difficulty":"Easy|Medium|Hard"}]}
- Exactly four options per question.
- answerIndex must be 0, 1, 2, or 3.
- Explanations must teach the student briefly and clearly.
- Match the requested class level; do not make advanced questions unless the difficulty requires it.
- Do not repeat or closely paraphrase excluded questions.
- Do not include markdown fences.

Excluded questions:
${excluded.map((q, i) => `${i + 1}. ${q}`).join('\n') || '(none)'}`;

  let response;
  try {
    response = await fetch(OPENROUTER_URL, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
        'HTTP-Referer': 'https://github.com/ttbluck38-sys/NARENDAR-AI',
        'X-Title': 'StudyPilot AI'
      },
      body: JSON.stringify({
        model: MODEL,
        temperature: 0.7,
        max_tokens: 5000,
        messages: [
          { role: 'system', content: 'Return only valid JSON matching the requested schema.' },
          { role: 'user', content: prompt }
        ]
      })
    });
  } catch (err) {
    logger.error('OpenRouter request failed', err);
    return json(res, 502, { error: 'AI provider connection failed.' });
  }

  const raw = await response.text();
  if (!response.ok) {
    logger.error('OpenRouter error', { status: response.status, body: raw.slice(0, 1000) });
    return json(res, 502, { error: `AI provider returned HTTP ${response.status}.` });
  }

  let outer;
  try {
    outer = JSON.parse(raw);
  } catch (_) {
    return json(res, 502, { error: 'AI provider returned invalid JSON.' });
  }

  const content = outer?.choices?.[0]?.message?.content;
  if (!content) return json(res, 502, { error: 'AI provider returned no content.' });

  let parsed;
  try {
    const cleaned = String(content).replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
    parsed = JSON.parse(cleaned);
  } catch (err) {
    logger.error('Model JSON parse failed', { content: String(content).slice(0, 1500) });
    return json(res, 502, { error: 'AI returned malformed question data.' });
  }

  const questions = Array.isArray(parsed.questions) ? parsed.questions : [];
  const valid = [];
  const seen = new Set(excluded);
  for (const item of questions) {
    const q = String(item?.question || '').trim();
    const options = Array.isArray(item?.options) ? item.options.map((x) => String(x || '').trim()) : [];
    const answerIndex = Number(item?.answerIndex);
    const key = normalize(q);
    if (!q || options.length !== 4 || options.some((x) => !x) || !Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 || seen.has(key)) continue;
    seen.add(key);
    valid.push({
      question: q,
      options,
      answerIndex,
      explanation: String(item?.explanation || '').trim(),
      topic: String(item?.topic || subject).trim(),
      difficulty: String(item?.difficulty || difficulty).trim()
    });
    if (valid.length >= count) break;
  }

  if (valid.length < count) {
    return json(res, 502, { error: `AI returned only ${valid.length} valid unique questions out of ${count}. Please retry.` });
  }

  return json(res, 200, { questions: valid });
}

exports.api = onRequest({
  region: 'us-central1',
  timeoutSeconds: 120,
  memory: '512MiB',
  invoker: 'public'
}, async (req, res) => {
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
  if (req.method === 'OPTIONS') return res.status(204).send('');

  if (req.path === '/functions/v1/study-generate-mcqs' || req.path === '/study-generate-mcqs') {
    return generateMcqs(req, res);
  }
  return json(res, 404, { error: 'StudyPilot API route not found.' });
});
