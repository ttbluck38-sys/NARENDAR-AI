package com.narendar.hub

/**
 * StudyPilot AI now uses Firebase Functions for its AI backend.
 * No Supabase project or client key is used by the app.
 * The legacy property names are kept temporarily so the existing quiz UI
 * can migrate without rewriting the large UI file in one change.
 */
internal object AiConfig {
    // Firebase HTTPS function. The function exposes /functions/v1/study-generate-mcqs.
    const val SUPABASE_URL = "https://us-central1-studypilot-ai-ac864.cloudfunctions.net/api"
    const val SUPABASE_PUBLISHABLE_KEY = "firebase-client"

    fun isConfigured(): Boolean =
        SUPABASE_URL.startsWith("https://us-central1-studypilot-ai-ac864.cloudfunctions.net/")
}
