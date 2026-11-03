import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { withSupabase } from "npm:@supabase/server@1";

const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const MODEL = "openrouter/free";
const clean = (v: unknown, max = 3000) => String(v ?? "").replace(/\s+/g, " ").trim().slice(0, max);

export default {
  fetch: withSupabase({ auth: "publishable" }, async (req) => {
    if (req.method !== "POST") return Response.json({ error: "POST required." }, { status: 405 });
    try {
      const apiKey = Deno.env.get("OPENROUTER_API_KEY")?.trim();
      if (!apiKey) return Response.json({ error: "OpenRouter backend key is not configured." }, { status: 503 });

      const body = await req.json().catch(() => ({})) as Record<string, unknown>;
      const question = clean(body.question, 1800);
      const options = Array.isArray(body.options) ? body.options.map((x: unknown) => clean(x, 500)).slice(0, 4) : [];
      const correctAnswer = clean(body.correctAnswer, 500);
      const selectedAnswer = clean(body.selectedAnswer, 500);
      const language = clean(body.language || "English", 40);

      if (!question || options.length !== 4 || !correctAnswer) {
        return Response.json({ error: "Question, four options, and correct answer are required." }, { status: 400 });
      }

      const upstream = await fetch(OPENROUTER_URL, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
          "HTTP-Referer": "https://github.com/ttbluck38-sys/NARENDAR-AI",
          "X-Title": "StudyPilot AI",
        },
        body: JSON.stringify({
          model: MODEL,
          temperature: 0.35,
          max_tokens: 900,
          messages: [
            { role: "system", content: "You are StudyPilot AI, a school tutor. Explain MCQs accurately and briefly. Never invent facts. Return only the explanation text, no JSON and no markdown headings." },
            { role: "user", content: `Language: ${language}\nQuestion: ${question}\nOptions: ${options.map((x: string, i: number) => `${String.fromCharCode(65 + i)}. ${x}`).join(" | ")}\nCorrect answer: ${correctAnswer}\nStudent selected: ${selectedAnswer || "No answer selected"}\n\nExplain why the correct answer is correct and, when a student answer is provided, briefly explain why it is right or wrong. Keep it suitable for a school student.` },
          ],
        }),
      });

      const raw = await upstream.text();
      if (!upstream.ok) return Response.json({ error: `AI provider returned HTTP ${upstream.status}.` }, { status: 502 });

      let outer: any;
      try { outer = JSON.parse(raw); } catch { return Response.json({ error: "AI provider returned invalid JSON." }, { status: 502 }); }
      const explanation = outer?.choices?.[0]?.message?.content;
      if (typeof explanation !== "string" || !explanation.trim()) return Response.json({ error: "AI provider returned no explanation." }, { status: 502 });

      return Response.json({ explanation: explanation.trim(), model: MODEL });
    } catch (error) {
      console.error("[studypilot-explain] request failed", error);
      return Response.json({ error: "StudyPilot explanation request could not be completed." }, { status: 500 });
    }
  }),
};
