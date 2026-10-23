import argparse, json, random, sys
from pathlib import Path
import torch
import torch.nn.functional as F
ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "src"))
from studypilot_ai.model import TinyLanguageModel

def load_corpus(path):
    texts = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            item = json.loads(line)
            if isinstance(item.get("text"), str):
                texts.append(item["text"].strip())
    if not texts:
        raise ValueError("Dataset is empty")
    return "\n\n".join(texts) + "\n"

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--steps", type=int, default=1200)
    p.add_argument("--batch-size", type=int, default=16)
    p.add_argument("--block-size", type=int, default=96)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--dataset", default=str(ROOT / "dataset/seed.jsonl"))
    p.add_argument("--out", default=str(ROOT / "artifacts/studypilot_tiny.pt"))
    a = p.parse_args()
    random.seed(a.seed); torch.manual_seed(a.seed)
    device = "cuda" if torch.cuda.is_available() else "cpu"
    text = load_corpus(Path(a.dataset))
    vocab = sorted(set(text)); stoi = {c:i for i,c in enumerate(vocab)}
    ids = torch.tensor([stoi[c] for c in text], dtype=torch.long)
    split = max(a.block_size + 2, int(len(ids) * 0.9))
    train_data = ids[:split]
    val_data = ids[split:] if split < len(ids) else ids
    model = TinyLanguageModel(len(vocab), block_size=a.block_size).to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4, weight_decay=0.01)
    def batch(source):
        max_start = max(1, len(source) - a.block_size - 1)
        starts = torch.randint(0, max_start, (a.batch_size,))
        x = torch.stack([source[int(i):int(i)+a.block_size] for i in starts]).to(device)
        y = torch.stack([source[int(i)+1:int(i)+a.block_size+1] for i in starts]).to(device)
        return x, y
    for step in range(1, a.steps + 1):
        model.train(); x,y = batch(train_data)
        loss = F.cross_entropy(model(x).reshape(-1,len(vocab)), y.reshape(-1))
        optimizer.zero_grad(set_to_none=True); loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0); optimizer.step()
        if step == 1 or step % 100 == 0 or step == a.steps:
            model.eval()
            with torch.no_grad():
                vx,vy = batch(val_data)
                vl = F.cross_entropy(model(vx).reshape(-1,len(vocab)),vy.reshape(-1))
            print(f"step={step}/{a.steps} train_loss={loss.item():.4f} val_loss={vl.item():.4f}", flush=True)
    out = Path(a.out); out.parent.mkdir(parents=True,exist_ok=True)
    torch.save({"state_dict":model.cpu().state_dict(),"vocab":vocab,"block_size":a.block_size,
        "d_model":128,"nhead":4,"layers":3,"corpus_chars":len(text),
        "warning":"Research prototype; not factually reliable without further training/evaluation."},out)
    (out.parent/"tokenizer.json").write_text(json.dumps({"vocab":vocab,"type":"character"},ensure_ascii=False,indent=2),encoding="utf-8")
    print(f"Saved checkpoint: {out}")
if __name__ == "__main__": main()
