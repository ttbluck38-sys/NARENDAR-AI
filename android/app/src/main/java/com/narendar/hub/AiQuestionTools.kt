package com.narendar.hub

import android.speech.tts.TextToSpeech
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import com.narendar.hub.ai.StudyPilotAIEngine

@Composable
internal fun AiQuestionTools(
    question: String,
    options: List<String>,
    correctAnswer: String,
    selectedAnswer: String?,
    appLanguage: String = "English",
    megaTest: Boolean = false,
    revealUses: Int = 0,
    onReveal: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember(appLanguage) { mutableStateOf(appLanguage) }
    var googleQuery by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
        }
    }

    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(14.dp)) {
            AppText("App language: $language", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = {
                    val locale = when (language) {
                        "Urdu" -> Locale("ur", "PK")
                        "Sindhi" -> Locale("sd", "PK")
                        else -> Locale.US
                    }
                    val engine = tts
                    if (engine == null || !ttsReady) {
                        error = "Speech engine is still starting. Try again in a moment."
                    } else {
                        val result = engine.setLanguage(locale)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            error = "This phone's speech engine does not support $language. Install compatible speech data."
                        } else {
                            loading = true
                            scope.launch {
                                val original = buildString {
                                    append(question)
                                    options.forEachIndexed { index, option ->
                                        append(". Option ${'A' + index}. ")
                                        append(option)
                                    }
                                }
                                val spoken = if (language == "English") original else StudyPilotWebSearch.translateForLanguage(original, language)
                                if (spoken.isNullOrBlank()) {
                                    error = "Translation to $language was unavailable; nothing was read aloud."
                                } else {
                                    engine.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "studypilot-question")
                                }
                                loading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { AppText("🔊 Read question + all options") }

            OutlinedButton(onClick = { if (!megaTest || revealUses < 10) { onReveal(); explanation = "Correct answer: $correctAnswer"; error = "" } else error = "All 10 answer reveals have been used for this mega test." }, enabled = !megaTest || revealUses < 10, modifier = Modifier.fillMaxWidth()) { AppText(if (megaTest) "Show Correct Answer (${10 - revealUses} left)" else "Show Correct Answer") }

            Button(
                onClick = {
                    loading = true
                    error = ""
                    explanation = ""
                    scope.launch {
                        try {
                            explanation = withContext(Dispatchers.Default) {
                                val prompt = question + ". Options: " + options.joinToString("; ")
                                val offline = StudyPilotAIEngine.ask(prompt, subject = "MCQ explanation", preferredLanguage = language)
                                if (offline.confidence >= 0.43 && !offline.needsClarification) {
                                    val verdict = when (language) {
                                        "Urdu" -> if (selectedAnswer == correctAnswer) "آپ کا جواب درست ہے۔ " else "درست جواب: " + correctAnswer + "۔ "
                                        "Sindhi" -> if (selectedAnswer == correctAnswer) "توهان جو جواب صحيح آهي. " else "صحيح جواب: " + correctAnswer + ". "
                                        else -> if (selectedAnswer == correctAnswer) "Your answer is correct. " else "Correct answer: " + correctAnswer + ". "
                                    }
                                    verdict + offline.text
                                } else {
                                    val web = StudyPilotWebSearch.search(question, language)
                                    if (web != null) {
                                        sourceUrl = web.sourceUrl
                                        val webText = if (language == "English") web.extract else StudyPilotWebSearch.translateForLanguage(web.extract, language)
                                        if (webText != null) "Web reference: " + web.title + "\n" + webText
                                        else { googleQuery = question; "A source was found but could not be translated. Open Google results below." }
                                    } else if (AiConfig.isConfigured()) {
                                        withContext(Dispatchers.IO) { requestExplanation(question, options, correctAnswer, selectedAnswer, language) }
                                    } else {
                                        googleQuery = question
                                        when (language) {
                                            "Urdu" -> "آف لائن علم اور ویب خلاصے سے جواب نہیں ملا۔ تازہ نتائج کے لیے نیچے گوگل تلاش کھولیں۔"
                                            "Sindhi" -> "آف لائن ڄاڻ يا ويب خلاصو جواب نه ڏئي سگهيو. نون نتيجن لاءِ هيٺ گوگل ڳولا کوليو."
                                            else -> "I could not verify an answer offline. Open Google results below for current information."
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "AI explanation failed. Check internet and retry."
                        } finally { loading = false }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) CircularProgressIndicator()
                else AppText(if (selectedAnswer == null) "✨ Explain this question with AI" else "✨ Why is my answer right or wrong?")
            }

            if (explanation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AppText(if (explanation.startsWith("Correct answer:")) "Answer reveal" else "StudyPilot AI • $language", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                AppText(explanation)
                if (sourceUrl.isNotBlank()) OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }, modifier = Modifier.fillMaxWidth()) { AppText("Open source") }
                if (googleQuery.isNotBlank()) OutlinedButton(onClick = { StudyPilotWebSearch.openGoogle(context, googleQuery) }, modifier = Modifier.fillMaxWidth()) { AppText("Search Google") }
                if (!explanation.startsWith("Correct answer:")) OutlinedButton(
                    onClick = {
                        val engine = tts
                        if (engine == null || !ttsReady) {
                            error = "Speech engine is still starting. Try again."
                        } else {
                            val locale = when (language) { "Urdu" -> Locale("ur", "PK"); "Sindhi" -> Locale("sd", "PK"); else -> Locale.US }
                            val result = engine.setLanguage(locale)
                            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                                error = "This phone speech engine does not support $language."
                            } else scope.launch {
                                val spoken = if (language == "English") explanation else StudyPilotWebSearch.translateForLanguage(explanation, language)
                                if (spoken.isNullOrBlank()) error = "Could not translate explanation to $language; nothing was spoken."
                                else engine.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "studypilot-explanation")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { AppText("🔊 Read AI explanation") }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                AppText(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun requestExplanation(
    question: String,
    options: List<String>,
    correctAnswer: String,
    selectedAnswer: String?,
    language: String
): String {
    val endpoint = AiConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/study-explain"
    val body = JSONObject().apply {
        put("question", question)
        put("options", JSONArray(options))
        put("correctAnswer", correctAnswer)
        put("selectedAnswer", selectedAnswer ?: JSONObject.NULL)
        put("language", language)
    }
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 30000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("apikey", AiConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer ${AiConfig.SUPABASE_PUBLISHABLE_KEY}")
    }
    try {
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = JSONObject(response.ifBlank { "{}" })
        if (code !in 200..299) {
            val message = json.optString("error", json.optString("message", "HTTP $code"))
            throw IllegalStateException("AI service error ($code): $message")
        }
        return json.optString("explanation").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("AI returned an empty explanation. Please retry.")
    } finally {
        connection.disconnect()
    }
}
