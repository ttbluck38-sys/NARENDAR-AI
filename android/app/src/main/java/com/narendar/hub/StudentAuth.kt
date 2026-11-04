package com.narendar.hub

import android.content.Context
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class StudentSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

private class AuthHttpException(
    val statusCode: Int,
    val errorCode: String,
    message: String
) : IOException(message)

internal object StudentAuth {
    private const val PREFS = "studypilot_auth"
    private val sessionLock = Any()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun token(context: Context): String =
        prefs(context).getString("access_token", "").orEmpty()

    fun userId(context: Context): String =
        prefs(context).getString("user_id", "").orEmpty()

    fun email(context: Context): String =
        prefs(context).getString("email", "").orEmpty()

    /**
     * Refresh-token rotation means a refresh token must not be exchanged by
     * two startup/profile requests at the same time. Serialize all refreshes
     * and re-read SharedPreferences after acquiring the lock.
     */
    fun restore(context: Context): Boolean = synchronized(sessionLock) {
        val stored = prefs(context)
        val refreshToken = stored.getString("refresh_token", "").orEmpty()
        if (refreshToken.isBlank()) return@synchronized false

        val expiry = stored.getLong("expires_at", 0L)
        if (token(context).isNotBlank() &&
            expiry > System.currentTimeMillis() / 1000 + 120
        ) {
            return@synchronized true
        }

        try {
            authRequest(
                context,
                "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refreshToken)
            )
            true
        } catch (error: AuthHttpException) {
            val invalidRefresh = error.errorCode.equals("invalid_grant", true) ||
                error.message.orEmpty().contains("invalid refresh token", true) ||
                error.message.orEmpty().contains("refresh token not found", true)

            if (invalidRefresh) {
                logout(context)
                false
            } else {
                // A server error is not proof that the saved session is invalid.
                token(context).isNotBlank()
            }
        } catch (_: IOException) {
            // Keep the cached session when the device is offline or the
            // Auth endpoint is temporarily unreachable.
            token(context).isNotBlank()
        } catch (_: Exception) {
            // Malformed/transient responses must not erase a student's session.
            token(context).isNotBlank()
        }
    }

    fun requireAccessToken(context: Context): String = synchronized(sessionLock) {
        val stored = prefs(context)
        val current = stored.getString("access_token", "").orEmpty()
        val expiry = stored.getLong("expires_at", 0L)

        if (current.isNotBlank() &&
            expiry > System.currentTimeMillis() / 1000 + 90
        ) {
            return@synchronized current
        }

        if (restore(context)) {
            val refreshed = prefs(context).getString("access_token", "").orEmpty()
            if (refreshed.isNotBlank()) return@synchronized refreshed
        }

        throw IllegalStateException(
            "Your StudyPilot session is not active. Please sign in again."
        )
    }

    fun logout(context: Context) {
        synchronized(sessionLock) {
            prefs(context).edit().clear().apply()
        }
    }

    fun signIn(context: Context, email: String, password: String): StudentSession =
        synchronized(sessionLock) {
            authRequest(
                context,
                "/auth/v1/token?grant_type=password",
                JSONObject().put("email", email.trim()).put("password", password)
            )
        }

    fun signUp(
        context: Context,
        email: String,
        password: String,
        name: String
    ): StudentSession = synchronized(sessionLock) {
        authRequest(
            context,
            "/auth/v1/signup",
            JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("data", JSONObject().put("display_name", name.trim()))
        )
    }

    private fun authRequest(
        context: Context,
        path: String,
        body: JSONObject
    ): StudentSession {
        val connection = (URL(AiConfig.SUPABASE_URL + path)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 25000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", AiConfig.SUPABASE_PUBLISHABLE_KEY)
        }

        try {
            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream
                else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            val json = try {
                JSONObject(raw.ifBlank { "{}" })
            } catch (_: Exception) {
                JSONObject()
            }

            if (status !in 200..299) {
                val message = json.optString(
                    "msg",
                    json.optString(
                        "message",
                        json.optString("error_description", "Sign in failed ($status).")
                    )
                )
                throw AuthHttpException(
                    status,
                    json.optString("error", json.optString("code", "")),
                    message
                )
            }

            val user = json.optJSONObject("user")
                ?: throw IllegalStateException(
                    "Check your email confirmation, then sign in."
                )

            val session = StudentSession(
                json.optString("access_token"),
                json.optString("refresh_token"),
                user.optString("id"),
                user.optString("email")
            )

            if (session.accessToken.isBlank() || session.userId.isBlank()) {
                throw IllegalStateException(
                    "Email confirmation may be required. Sign in after confirming your email."
                )
            }

            prefs(context).edit()
                .putString("access_token", session.accessToken)
                .putString("refresh_token", session.refreshToken)
                .putString("user_id", session.userId)
                .putString("email", session.email)
                .putLong(
                    "expires_at",
                    System.currentTimeMillis() / 1000 +
                        json.optLong("expires_in", 3600)
                )
                .apply()

            return session
        } finally {
            connection.disconnect()
        }
    }

    fun saveProfile(
        context: Context,
        name: String,
        grade: Int,
        language: String,
        preferences: JSONObject = JSONObject(),
        memory: JSONObject = JSONObject()
    ) {
        val body = JSONObject()
            .put("user_id", userId(context))
            .put("display_name", name)
            .put("grade", grade.coerceIn(1, 12))
            .put("preferred_language", language)
            .put("learning_preferences", preferences)
            .put("student_memory", memory)
            .put("updated_at", java.time.Instant.now().toString())

        request(
            context,
            "/rest/v1/student_profiles?on_conflict=user_id",
            "POST",
            body,
            "resolution=merge-duplicates,return=minimal"
        )
    }

    fun logEvent(
        context: Context,
        type: String,
        subject: String,
        topic: String,
        question: String,
        selected: String = "",
        correct: String = "",
        isCorrect: Boolean? = null,
        details: JSONObject = JSONObject()
    ) {
        if (userId(context).isBlank()) return

        val body = JSONObject()
            .put("user_id", userId(context))
            .put("event_type", type)
            .put("subject", subject)
            .put("topic", topic)
            .put("question", question)
            .put("selected_answer", selected)
            .put("correct_answer", correct)
            .put("details", details)

        if (isCorrect != null) body.put("is_correct", isCorrect)

        request(context, "/rest/v1/learning_events", "POST", body, "return=minimal")
    }

    fun request(
        context: Context,
        path: String,
        method: String,
        body: JSONObject?,
        prefer: String? = null
    ): String {
        val accessToken = requireAccessToken(context)
        val connection = (URL(AiConfig.SUPABASE_URL + path)
            .openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 30000
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", AiConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $accessToken")
            if (prefer != null) setRequestProperty("Prefer", prefer)
            if (body != null) doOutput = true
        }

        try {
            if (body != null) {
                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream
                else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                val message = try {
                    JSONObject(raw.ifBlank { "{}" }).optString("message", "Try again.")
                } catch (_: Exception) {
                    "Try again."
                }
                throw IllegalStateException("Profile sync failed ($status): $message")
            }

            return raw
        } finally {
            connection.disconnect()
        }
    }
}
