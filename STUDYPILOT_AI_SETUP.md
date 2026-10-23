# StudyPilot AI — district demo setup

This branch adds AI explanations to the quiz and a text-to-speech button for the question and every option. Keep `main` unchanged while testing.

## 1. Add GitHub Actions secrets

Open **Settings → Secrets and variables → Actions → New repository secret**:
https://github.com/ttbluck38-sys/NARENDAR-AI/settings/secrets/actions

Create these secrets (the names must match exactly):

| Secret name | Where it comes from | Purpose |
|---|---|---|
| `SUPABASE_URL` | Supabase project URL, e.g. `https://YOUR_PROJECT_REF.supabase.co` | Used when building the Android APK |
| `SUPABASE_PUBLISHABLE_KEY` | Supabase Project Settings → API Keys → publishable key (or legacy anon key) | Public client key used by the app |
| `SUPABASE_ACCESS_TOKEN` | Supabase account access tokens | Lets the deploy workflow manage this project's function |
| `SUPABASE_PROJECT_REF` | Project reference in Supabase project URL / General settings | Tells deployment which project to update |
| `OPENROUTER_API_KEY` | OpenRouter account → Keys | Secret used only by the Edge Function |

Never put `OPENROUTER_API_KEY` in Kotlin, Android resources, the APK, or a public repository. Never paste secret values into chat or screenshots. The Supabase access token is also sensitive.

Useful pages:
- Supabase dashboard: https://supabase.com/dashboard
- Supabase account tokens: https://supabase.com/dashboard/account/tokens
- OpenRouter keys: https://openrouter.ai/settings/keys
- OpenRouter free model directory: https://openrouter.ai/models?pricing=free

## 2. Deploy the AI backend

After all five GitHub secrets are saved:
1. Open the repository Actions tab: https://github.com/ttbluck38-sys/NARENDAR-AI/actions
2. Select **Deploy StudyPilot AI backend**.
3. Click **Run workflow** and choose branch `studypilot-mcq-fix`.
4. Wait for the workflow to finish successfully.

That workflow stores `OPENROUTER_API_KEY` as a Supabase Edge Function secret and deploys `supabase/functions/study-explain/index.ts`. The key never goes to the phone.

## 3. Build the Android APK

1. In Actions, select **Android Build**.
2. Click **Run workflow** and choose `studypilot-mcq-fix`.
3. Wait for the build to finish. Open the successful run, scroll to **Artifacts**, and download `NarendarHub-debug-apk`.
4. Extract the ZIP to find `app-debug.apk`, then install it on the demo phone.

The workflow injects `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` during the build. The values are not committed back to the source file. If you add or change these secrets, manually run Android Build again.

## 4. Test on the phone

- Start a quiz and choose an answer.
- Check the existing correct/incorrect feedback.
- Tap **Why is my answer right or wrong?** and test English, Urdu and Sindhi.
- Tap **Read question + all options** and check the device's speech engine.
- Try **Read AI explanation** if the explanation appears.

Urdu/Sindhi speech availability depends on the text-to-speech voices installed on the phone. AI requests require internet and may be rate-limited or temporarily unavailable because free model availability can change.

## Demo security note

The Edge Function is configured without Supabase JWT verification so it can be called by the demo app using a publishable/anon key. This endpoint is therefore publicly callable; use it only for the controlled demo, monitor OpenRouter usage, and do not publish the app widely until proper user authentication and server-side rate limiting are added. The Supabase publishable/anon key is not the OpenRouter secret.

## Files changed

- `android/app/src/main/java/com/narendar/hub/AiQuestionTools.kt` — language selection, question/options read-aloud, AI explanation request.
- `android/app/src/main/java/com/narendar/hub/AiConfig.kt` — build-time Supabase configuration placeholders.
- `android/app/src/main/java/com/narendar/hub/NarendarFinalFixed.kt` — Quiz screen integration.
- `android/app/src/main/AndroidManifest.xml` — internet permission.
- `supabase/functions/study-explain/index.ts` — server-side OpenRouter call.
- `supabase/config.toml` — function configuration.
- `.github/workflows/android-build.yml` — optional build-time Supabase config injection.
- `.github/workflows/study-explain-deploy.yml` — manual backend deployment workflow.

A successful CI build only proves the APK compiled. The AI backend still needs deployment, and the final phone checks above are needed before calling the demo fully verified.
