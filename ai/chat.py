import argparse, sys
from pathlib import Path
import torch
ROOT=Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / "src"))
from studypilot_ai.model import TinyLanguageModel

def main():
    p=argparse.ArgumentParser(); p.add_argument("--checkpoint",default=str(ROOT/"artifacts/studypilot_tiny.pt")); p.add_argument("--max-new-tokens",type=int,default=180); a=p.parse_args()
    ckpt=torch.load(a.checkpoint,map_location="cpu",weights_only=False); vocab=ckpt["vocab"]; stoi={c:i for i,c in enumerate(vocab)}
    model=TinyLanguageModel(len(vocab),block_size=ckpt["block_size"],d_model=ckpt["d_model"],nhead=ckpt["nhead"],layers=ckpt["layers"])
    model.load_state_dict(ckpt["state_dict"]); model.eval()
    print("StudyPilot Tiny experimental sampler. Output may be nonsense; do not use as factual tutor.")
    while True:
        try: prompt=input("You> ")
        except EOFError: break
        if prompt.strip().lower() in {"exit","quit"}: break
        prefix="Student: "+prompt+"\nStudyPilot:"
        ids=[stoi.get(c,stoi.get(" ",0)) for c in prefix]
        with torch.no_grad(): result=model.generate(torch.tensor([ids]),a.max_new_tokens,0.7)[0].tolist()
        print("AI> "+"".join(vocab[i] for i in result[len(ids):]).split("\n")[0])
if __name__=="__main__": main()
