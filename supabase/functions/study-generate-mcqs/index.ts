import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "npm:@supabase/server@1";

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const MODEL = "openrouter/free";
const clean = (v: unknown, max = 2000) => String(v ?? "").replace(/\s+/g, " ").trim().slice(0, max);
const normalize = (v: unknown) => clean(v, 3000).toLowerCase();

function buildPrompt(body: Record<string, unknown>, count: number, excluded: string[]) {
  const grade = Math.max(1, Math.min(12, Number(body.grade || 8)));
  const subject = clean(body.subject || "Mathematics", 80);
  const difficulty = clean(body.difficulty || "Mixed", 30);
  const language = clean(body.language || "English", 40);
  const studentName = clean(body.studentName || "Student", 80);
  const learningLevel = clean(body.learningLevel || "", 300);
  return `You are StudyPilot AI, a careful school-learning assistant. Generate exactly ${count} unique multiple-choice questions.\nStudent: ${studentName}\nClass: ${grade}\nSubject: ${subject}\nDifficulty: ${difficulty}\nLanguage: ${language}\nLearning context: ${learningLevel}\n\nRules:\n- Return ONLY one valid JSON object.\n- Schema: {"questions":[{"question":"...","options":["A","B","C","D"],"answerIndex":0,"explanation":"...","topic":"...","difficulty":"Easy|Medium|Hard"}]}\n- Exactly four non-empty options per question.\n- answerIndex must be 0, 1, 2, or 3.\n- Explanations should teach the student briefly and clearly.\n- Match the requested class level.\n- Do not repeat or closely paraphrase excluded questions.\n- Do not include markdown fences, commentary, or extra text.\n\nExcluded questions:\n${excluded.map((q, i) => `${i + 1}. ${q}`).join("\n") || "(none)"}`;
}

function extractJsonObject(text: string): string {
  const cleaned = text.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("No JSON object found.");
  return cleaned.slice(start, end + 1);
}

function parseQuestions(content: string, count: number, subject: string, difficulty: string, excluded: string[]) {
  const parsed = JSON.parse(extractJsonObject(content));
  const items = Array.isArray(parsed?.questions) ? parsed.questions : [];
  const seen = new Set(excluded.map(normalize));
  const valid: unknown[] = [];
  for (const item of items) {
    const question = clean(item?.question, 1200);
    const options = Array.isArray(item?.options) ? item.options.map((x: unknown) => clean(x, 500)) : [];
    const answerIndex = Number(item?.answerIndex);
    const key = normalize(question);
    if (!question || options.length !== 4 || options.some((x: string) => !x) || new Set(options.map(normalize)).size !== 4 || !Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 || seen.has(key)) continue;
    seen.add(key);
    valid.push({ question, options, answerIndex, explanation: clean(item?.explanation, 1200), topic: clean(item?.topic || subject, 200), difficulty: clean(item?.difficulty || difficulty, 30) });
    if (valid.length >= count) break;
  }
  return valid;
}

async function requestBatch(body: Record<string, unknown>, count: number, excluded: string[], apiKey: string) {
  const upstream = await fetch(OPENROUTER_URL, {
    method: "POST",
    headers: { Authorization: `Bearer ${apiKey}`, "Content-Type": "application/json", "HTTP-Referer": "https://github.com/ttbluck38-sys/NARENDAR-AI", "X-Title": "StudyPilot AI" },
    body: JSON.stringify({ model: MODEL, temperature: 0.65, max_tokens: Math.min(7000, 700 * count), messages: [
      { role: "system", content: "You are StudyPilot AI. Return only valid JSON matching the requested MCQ schema." },
      { role: "user", content: buildPrompt(body, count, excluded) },
    ] }),
  });
  const raw = await upstream.text();
  if (!upstream.ok) throw new Error(`AI provider returned HTTP ${upstream.status}.`);
  let outer: any;
  try { outer = JSON.parse(raw); } catch { throw new Error("AI provider returned invalid JSON."); }
  const content = outer?.choices?.[0]?.message?.content;
  if (typeof content !== "string" || !content.trim()) throw new Error("AI provider returned no question content.");
  return parseQuestions(content, count, clean(body.subject || "Mathematics", 80), clean(body.difficulty || "Mixed", 30), excluded);
}

export default {
  fetch: withSupabase({ auth: "publishable" }, async (req) => {
    if (req.method !== "POST") return Response.json({ error: "POST required." }, { status: 405 });
    try {
      const apiKey = Deno.env.get("OPENROUTER_API_KEY_2")?.trim();
      if (!apiKey) return Response.json({ error: "StudyPilot OpenRouter backend key is not configured." }, { status: 503 });
      const body = await req.json().catch(() => ({})) as Record<string, unknown>;
      const count = Math.max(1, Math.min(10, Number(body.count || 10)));
      const excluded = Array.isArray(body.excludeQuestions) ? body.excludeQuestions.map(normalize).filter(Boolean).slice(0, 100) : [];
      let lastError = "AI could not generate enough valid questions.";
      for (let attempt = 0; attempt < 2; attempt++) {
        try {
          const questions = await requestBatch(body, count, excluded, apiKey);
          if (questions.length >= count) return Response.json({ questions, model: MODEL });
          lastError = `AI returned only ${questions.length} valid unique questions out of ${count}.`;
        } catch (error) {
          lastError = error instanceof Error ? error.message : "AI request failed.";
        }
      }
      return Response.json({ error: `${lastError} Please retry.` }, { status: 502 });
    } catch (error) {
      console.error("[studypilot-mcq] request failed", error);
      return Response.json({ error: "StudyPilot AI request could not be completed." }, { status: 500 });
    }
  }),
};
