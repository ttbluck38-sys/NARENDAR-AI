package com.narendar.hub

/** StudyPilot AI backend configuration. */
internal object AiConfig {
    // Supabase Edge Function. The existing ai-chat slug now serves StudyPilot MCQ generation.
    const val SUPABASE_URL = "https://coogryjidljqyzzsymte.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_sd9KlBr1NPwmf1hQJvx8fQ_G3Z28H2z"

    fun isConfigured(): Boolean =
        SUPABASE_URL == "https://coogryjidljqyzzsymte.supabase.co" &&
            SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_")
}
