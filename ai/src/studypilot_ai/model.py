"""A compact character-level Transformer LM; experimental, not a frontier assistant."""
import torch
from torch import nn

class TinyLanguageModel(nn.Module):
    def __init__(self, vocab_size: int, block_size: int = 128, d_model: int = 128,
                 nhead: int = 4, layers: int = 3, dropout: float = 0.1):
        super().__init__()
        self.block_size = block_size
        self.token_embedding = nn.Embedding(vocab_size, d_model)
        self.position_embedding = nn.Embedding(block_size, d_model)
        layer = nn.TransformerEncoderLayer(d_model=d_model, nhead=nhead,
            dim_feedforward=d_model * 4, dropout=dropout, batch_first=True,
            norm_first=True, activation="gelu")
        self.transformer = nn.TransformerEncoder(layer, num_layers=layers)
        self.norm = nn.LayerNorm(d_model)
        self.lm_head = nn.Linear(d_model, vocab_size, bias=False)
        self.lm_head.weight = self.token_embedding.weight

    def forward(self, tokens: torch.Tensor) -> torch.Tensor:
        _, length = tokens.shape
        if length > self.block_size:
            tokens = tokens[:, -self.block_size:]
            length = tokens.shape[1]
        positions = torch.arange(length, device=tokens.device).unsqueeze(0)
        x = self.token_embedding(tokens) + self.position_embedding(positions)
        mask = torch.triu(torch.ones(length, length, device=tokens.device, dtype=torch.bool), diagonal=1)
        x = self.transformer(x, mask=mask)
        return self.lm_head(self.norm(x))

    @torch.no_grad()
    def generate(self, tokens: torch.Tensor, max_new_tokens: int = 160, temperature: float = 0.8) -> torch.Tensor:
        self.eval()
        for _ in range(max_new_tokens):
            context = tokens[:, -self.block_size:]
            logits = self(context)[:, -1, :] / max(temperature, 0.05)
            tokens = torch.cat((tokens, torch.multinomial(torch.softmax(logits, dim=-1), 1)), dim=1)
        return tokens
