package com.narendar.hub

/**
 * Set these two values to your Supabase project URL and publishable/anon key.
 * Never put the OpenRouter API key in this Android file.
 */
internal object AiConfig {
    const val SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY"

    fun isConfigured(): Boolean =
        SUPABASE_URL.startsWith("https://") &&
        !SUPABASE_URL.contains("YOUR_PROJECT_REF") &&
        SUPABASE_PUBLISHABLE_KEY.isNotBlank() &&
        !SUPABASE_PUBLISHABLE_KEY.startsWith("YOUR_")
}
