package com.narendar.hub

import android.content.Intent
import android.net.Uri
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StudyWebAnswer(val title: String, val extract: String, val sourceUrl: String)

object StudyPilotWebSearch {
    suspend fun translateForLanguage(text: String, language: String): String? = withContext(Dispatchers.IO) {
        if (language == "English") return@withContext text
        val target = if (language == "Urdu") "ur" else "sd"
        val targetChars = if (language == "Urdu") Regex("[\\u0600-\\u06ff]") else Regex("[ٻڄڙڏڳڱڻٽٿڀڦڪ\\u0600-\\u06ff]")
        if (targetChars.containsMatchIn(text)) return@withContext text
        val encoded = URLEncoder.encode(text.take(480), "UTF-8")
        val endpoint = "https://api.mymemory.translated.net/get?q=$encoded&langpair=en|$target"
        try {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "StudyPilotAI/1.0")
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val translated = json.optJSONObject("responseData")?.optString("translatedText").orEmpty().trim()
                if (translated.isBlank() || translated.equals(text, ignoreCase = true) || json.optInt("responseStatus", 200) !in 200..299) null else translated
            } finally { connection.disconnect() }
        } catch (_: Exception) { null }
    }

    suspend fun search(query: String, language: String): StudyWebAnswer? = withContext(Dispatchers.IO) {
        val wiki = when (language) { "Urdu" -> "ur"; "Sindhi" -> "sd"; else -> "en" }
        val encoded = URLEncoder.encode(query.take(180), "UTF-8")
        val endpoint = "https://$wiki.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrlimit=1&prop=extracts&exintro=1&explaintext=1&format=json"
        try {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 5500
                setRequestProperty("User-Agent", "StudyPilotAI/1.0 (educational app)")
            }
            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val pages = json.optJSONObject("query")?.optJSONObject("pages") ?: return@withContext null
                val keys = pages.keys()
                if (!keys.hasNext()) return@withContext null
                val page = pages.optJSONObject(keys.next()) ?: return@withContext null
                val extract = page.optString("extract").trim()
                if (extract.length < 40) return@withContext null
                val title = page.optString("title", query)
                StudyWebAnswer(title, extract.take(1800), "https://$wiki.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8"))
            } finally { connection.disconnect() }
        } catch (_: Exception) { null }
    }

    fun openGoogle(context: Context, query: String) {
        val url = "https://www.google.com/search?q=" + URLEncoder.encode(query.take(250), "UTF-8")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
