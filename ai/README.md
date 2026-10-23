# StudyPilot AI — local model lab

This folder contains a **from-scratch experimental language model** and an offline Android tutor baseline.

## Honest capability boundary
- `src/studypilot_ai/model.py`: small character-level Transformer language model.
- `train.py`: trains model weights on the included educational text corpus.
- `evaluate.py`: technical smoke checks, not proof of accuracy.
- `android/StudyPilotAIEngine.kt`: typo-tolerant retrieval tutor with curated explanations. It is not the trained Transformer.
- The PyTorch checkpoint is **not yet Android-runnable**. It needs compatible export/runtime, evaluation, and device testing before use as the generative brain.

No API key or external AI service is used. This prototype is not ChatGPT-level and can generate nonsense.

## Train
Python 3.10+:
```bash
pip install -r requirements.txt
python train.py --steps 1200
python evaluate.py --checkpoint artifacts/studypilot_tiny.pt
python chat.py --checkpoint artifacts/studypilot_tiny.pt
```

Run GitHub Actions → **Train StudyPilot Tiny Model** → Run workflow to train on a temporary free runner and upload a 30-day artifact.

## Milestones
1. Expand and review legally usable educational data.
2. Evaluate typo handling, factuality, and answer quality.
3. Export to a supported mobile runtime and match tokenizer exactly.
4. Integrate tutor UI and test on a real Android 8.1 device.
5. Do not describe a tiny prototype as equivalent to a frontier assistant.
