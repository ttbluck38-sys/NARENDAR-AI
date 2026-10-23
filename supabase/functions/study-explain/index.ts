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

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Use POST." }, 405);

  const apiKey = Deno.env.get("OPENROUTER_API_KEY");
  if (!apiKey) return json({ error: "Server AI key is not configured yet." }, 503);

  let input: Record<string, unknown>;
  try {
    input = await req.json();
  } catch {
    return json({ error: "Request body must be valid JSON." }, 400);
  }

  const question = typeof input.question === "string" ? input.question.trim() : "";
  const options = Array.isArray(input.options)
    ? input.options.filter((v): v is string => typeof v === "string").slice(0, 8)
    : [];
  const correctAnswer = typeof input.correctAnswer === "string" ? input.correctAnswer.trim() : "";
  const selectedAnswer = typeof input.selectedAnswer === "string" ? input.selectedAnswer.trim() : "";
  const allowedLanguages = ["English", "Urdu", "Sindhi"];
  const language = typeof input.language === "string" && allowedLanguages.includes(input.language)
    ? input.language
    : "English";

  if (!question || question.length > 2000 || options.length < 2 ||
      options.some((o) => o.length > 500) || !correctAnswer || correctAnswer.length > 500 ||
      selectedAnswer.length > 500) {
    return json({ error: "Question data is missing or too long." }, 400);
  }

  const systemPrompt = [
    "You are StudyPilot AI, a patient school tutor for middle-school learners.",
    "Explain the concept accurately and in age-appropriate language.",
    "Do not simply repeat the answer. Explain the reasoning in small clear steps.",
    "If the student's selected answer is wrong, gently explain the misconception and why the correct answer fits.",
    "If no answer was selected, explain how to solve the question.",
    "Do not invent curriculum facts; if the question is ambiguous, say so.",
    "Reply only in the requested language: " + language + ".",
    "Keep the answer concise, ideally 80-150 words."
  ].join(" ");

  const userPrompt = [
    "Question: " + question,
    "Options: " + options.map((o, i) => String.fromCharCode(65 + i) + ". " + o).join("\n"),
    "Correct answer: " + correctAnswer,
    "Student selected: " + (selectedAnswer || "No answer selected"),
    "Explain why the correct answer is correct and address the student's choice."
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
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt },
        ],
        temperature: 0.25,
        max_tokens: 450,
      }),
    });

    const data = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      const detail = typeof data?.error?.message === "string" ? data.error.message : "AI provider request failed.";
      console.error("OpenRouter request failed with status", upstream.status);
      return json({ error: detail.slice(0, 240) }, upstream.status === 429 ? 429 : 502);
    }
    const explanation = data?.choices?.[0]?.message?.content;
    if (typeof explanation !== "string" || !explanation.trim()) {
      return json({ error: "The selected free model returned no explanation. Try again shortly." }, 502);
    }
    return json({ explanation: explanation.trim() });
  } catch (error) {
    console.error("StudyPilot AI request failed", error instanceof Error ? error.message : "unknown error");
    return json({ error: "Could not reach the AI provider. Check the connection and retry." }, 502);
  }
});
