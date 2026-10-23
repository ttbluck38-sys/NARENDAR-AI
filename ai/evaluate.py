import argparse, json
from pathlib import Path
import torch
from studypilot_ai.model import TinyLanguageModel
ROOT=Path(__file__).resolve().parent

def main():
    p=argparse.ArgumentParser(); p.add_argument("--checkpoint",default=str(ROOT/"artifacts/studypilot_tiny.pt")); p.add_argument("--dataset",default=str(ROOT/"dataset/seed.jsonl")); a=p.parse_args()
    ckpt=torch.load(a.checkpoint,map_location="cpu",weights_only=False)
    vocab=ckpt["vocab"]
    model=TinyLanguageModel(len(vocab),block_size=ckpt["block_size"],d_model=ckpt["d_model"],nhead=ckpt["nhead"],layers=ckpt["layers"])
    model.load_state_dict(ckpt["state_dict"]); model.eval(); stoi={c:i for i,c in enumerate(vocab)}
    with torch.no_grad():
        ids=[stoi.get(c,stoi.get(" ",0)) for c in "Photosynthesis is"]
        logits=model(torch.tensor([ids]))
        assert logits.shape==(1,len(ids),len(vocab)) and torch.isfinite(logits).all()
    rows=[json.loads(x) for x in Path(a.dataset).read_text(encoding="utf-8").splitlines() if x.strip()]
    assert len(rows)>=20, f"Expected at least 20 examples; found {len(rows)}"
    assert all(isinstance(r.get("text"),str) and len(r["text"])>20 for r in rows)
    print(f"PASS: checkpoint loads; logits finite; {len(rows)} corpus rows; vocab={len(vocab)}")
    print("LIMIT: technical smoke test only, not accuracy or factuality certification.")
if __name__=="__main__": main()
