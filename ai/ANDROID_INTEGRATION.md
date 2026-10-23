# Android integration plan

## Current deliverable
`ai/android/StudyPilotAIEngine.kt` is a pure-Kotlin, typo-tolerant offline baseline for a small curated set of topics. It does not run the Python Transformer.

## Step 1 — Add engine source
Copy `ai/android/StudyPilotAIEngine.kt` to:
`android/app/src/main/java/com/narendar/hub/ai/StudyPilotAIEngine.kt`

Package and namespace match the current Android project. No API key or dependency is needed.

## Step 2 — Call it from a tutor screen
```kotlin
val answer = StudyPilotAIEngine.ask(
    message = userQuestion,
    subject = selectedSubject,
    classLevel = selectedClass
)
// Display answer.topic and answer.text.
// If answer.needsClarification is true, offer a rephrase prompt.
```

## Step 3 — Test before shipping
- `what is photosynthesis` → Photosynthesis
- `what is fotosinthesis` → Photosynthesis
- `plants food kaise banate hain` → Photosynthesis
- unknown topic → clarification, not a fabricated fact

## Step 4 — Train the research model
Follow `ai/README.md`. GitHub Actions uploads a PyTorch checkpoint. It is not directly runnable inside Android.

## Step 5 — Mobile inference still needs engineering
Choose and validate an Android runtime/export path (e.g. ONNX Runtime Mobile or TensorFlow Lite if operators are supported), make the tokenizer exactly match training, quantize if feasible, and benchmark RAM, speed, APK size, and output quality.

## Step 6 — APK and device verification
Run Android Build, install the artifact on Android 8.1, then test typos, unknown questions, airplane mode, memory, speed, and answer quality. A successful compile is not proof of factual correctness.
