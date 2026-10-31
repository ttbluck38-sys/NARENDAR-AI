const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

const clean = (value: string) => value.toLowerCase().normalize("NFKC")
  .replace(/[^\p{L}\p{N}]+/gu, " ").trim().replace(/\s+/g, " ");

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Use POST." }, 405);

  const apiKey = Deno.env.get("OPENROUTER_API_KEY");
  if (!apiKey) return json({ error: "Server AI key is not configured." }, 503);

  let input: Record<string, unknown>;
  try { input = await req.json(); } catch { return json({ error: "Body must be valid JSON." }, 400); }

  const grade = Number(input.grade);
  const subject = typeof input.subject === "string" ? input.subject.trim().slice(0, 80) : "";
  const difficulty = typeof input.difficulty === "string" ? input.difficulty : "Mixed";
  const language = typeof input.language === "string" ? input.language : "English";
  const topic = typeof input.topic === "string" ? input.topic.trim().slice(0, 160) : "";
  const count = Math.max(1, Math.min(10, Number(input.count) || 10));
  const allowedDifficulty = ["Easy", "Medium", "Hard", "Mixed"];
  const allowedLanguage = ["English", "Urdu", "Sindhi"];
  const excluded = Array.isArray(input.excludeQuestions)
    ? input.excludeQuestions.filter((v): v is string => typeof v === "string").slice(0, 100).map(clean)
    : [];

  if (!Number.isInteger(grade) || grade < 1 || grade > 10 || !subject ||
      !allowedDifficulty.includes(difficulty) || !allowedLanguage.includes(language) ||
      excluded.some((q) => q.length > 500)) {
    return json({ error: "Provide grade 1–10, subject, valid difficulty and language." }, 400);
  }

  const system = [
    "You are StudyPilot AI, an expert school assessment writer.",
    "Create original, accurate, age-appropriate multiple-choice questions.",
    "Return ONLY a JSON object: { questions: [{ question, options: [four strings], answerIndex: integer 0..3, explanation, topic, difficulty }] }.",
    "Every question must have exactly four distinct options and exactly one defensible correct answer.",
    "Never repeat a question or merely reword an excluded question.",
    "Do not use trick wording, ambiguous facts, or answer patterns that reveal the key.",
    "Match the requested school grade and subject. Keep language consistent with the requested language.",
    "Explanations must teach the reasoning, not just state the answer.",
    "Do not include markdown fences or text outside the JSON object."
  ].join(" ");

  const user = [
    `Generate exactly ${count} fresh MCQs.`,
    `Class: ${grade}`,
    `Subject: ${subject}`,
    `Difficulty: ${difficulty}`,
    `Topic preference: ${topic || "cover varied appropriate topics"}`,
    `Language: ${language}`,
    excluded.length ? "Do not use questions equivalent to these recent questions: " + excluded.slice(0, 40).join(" | ") : "Avoid repeating questions within this batch."
  ].join("\n");

  try {
    const upstream = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://studypilot-ai.local",
        "X-Title": "StudyPilot AI",
      },
      body: JSON.stringify({
        model: "openrouter/free",
        messages: [{ role: "system", content: system }, { role: "user", content: user }],
        temperature: 0.8,
        max_tokens: Math.min(7000, 500 + count * 450),
        response_format: { type: "json_object" },
      }),
      signal: AbortSignal.timeout(55000),
    });

    const data = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      const detail = typeof data?.error?.message === "string" ? data.error.message : "Free AI provider is busy. Retry shortly.";
      return json({ error: detail.slice(0, 220), retryable: upstream.status === 429 || upstream.status >= 500 }, 502);
    }

    const raw = data?.choices?.[0]?.message?.content;
    if (typeof raw !== "string" || !raw.trim()) return json({ error: "AI returned an empty question set.", retryable: true }, 502);

    let parsed: { questions?: unknown[] };
    try { parsed = JSON.parse(raw); } catch { return json({ error: "AI response was not valid JSON. Retry the generation.", retryable: true }, 502); }

    const seen = new Set(excluded);
    const questions = (Array.isArray(parsed.questions) ? parsed.questions : []).flatMap((item) => {
      if (!item || typeof item !== "object") return [];
      const q = item as Record<string, unknown>;
      const question = typeof q.question === "string" ? q.question.trim() : "";
      const options = Array.isArray(q.options) ? q.options.filter((v): v is string => typeof v === "string").map(v => v.trim()) : [];
      const answerIndex = Number(q.answerIndex);
      const explanation = typeof q.explanation === "string" ? q.explanation.trim() : "";
      const fingerprint = clean(question);
      if (question.length < 8 || question.length > 700 || options.length !== 4 ||
          options.some(v => !v || v.length > 300) || new Set(options.map(clean)).size !== 4 ||
          !Number.isInteger(answerIndex) || answerIndex < 0 || answerIndex > 3 ||
          !explanation || explanation.length > 1200 || seen.has(fingerprint)) return [];
      seen.add(fingerprint);
      return [{
        question, options, answerIndex, explanation,
        topic: typeof q.topic === "string" ? q.topic.slice(0, 100) : subject,
        difficulty: allowedDifficulty.includes(String(q.difficulty)) ? q.difficulty : difficulty,
      }];
    });

    if (questions.length !== count) {
      return json({ error: `AI produced ${questions.length} valid unique questions; ${count} were requested. Retry to generate a clean set.`, retryable: true }, 502);
    }
    return json({ questions, model: "openrouter/free", generated: questions.length });
  } catch (error) {
    console.error("MCQ generation failed", error instanceof Error ? error.message : "unknown");
    return json({ error: "AI provider timed out or could not be reached. Retry shortly.", retryable: true }, 502);
  }
});
