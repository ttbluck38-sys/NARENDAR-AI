# StudyPilot AI — current setup

## Backend

- Supabase project: `coogryjidljqyzzsymte`
- Project URL: `https://coogryjidljqyzzsymte.supabase.co`
- Android package: `com.company.studypilotai`
- OpenRouter model: `openrouter/free`

The Android app uses the Supabase publishable key. The OpenRouter API key must stay in Supabase Edge Function Secrets and must never be placed in Kotlin, Android resources, GitHub source, or the APK.

Required Supabase Edge Function secret:

- `OPENROUTER_API_KEY_2`

The following StudyPilot functions read that secret:

- `study-generate-mcqs` — generates MCQs.
- `study-explain` — explains an MCQ.
 
Do not change or redeploy `scamshield-check`; it belongs to ScamShield.

## Android build

The Android Build workflow compiles the app and uploads `StudyPilot-AI-debug-apk`. It does not require Firebase configuration or GitHub Actions secrets. It can be run manually from the repository Actions tab.

## Current limitations

- The app currently stores the learner profile and progress locally on the device. It does not yet provide Supabase email/password signup or cross-device account sync.
- The tutor screen currently uses its offline knowledge engine and Wikipedia lookup; it is not yet connected to a general-purpose OpenRouter chat endpoint.
- The Edge Functions currently allow calls using the public publishable key without a signed-in user. This is suitable only for a limited demo; add Supabase Auth and server-side rate limits before public release.

## Test checklist

1. Open the app and select a class.
2. Generate a short MCQ quiz.
3. Submit an answer and check feedback.
4. Request an AI explanation.
5. Test English, Urdu and Sindhi.
6. Check the Android app on the target Android 8.1 phone.

A successful CI build proves compilation only. The quiz and explanation endpoints still need real-device testing.
