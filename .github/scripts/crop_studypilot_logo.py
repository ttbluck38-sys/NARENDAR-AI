#!/usr/bin/env python3
"""Create a launcher-ready StudyPilot logo crop from the repository's three-logo image."""
import sys
from pathlib import Path
from PIL import Image, ImageOps

src = Path(sys.argv[1])
dst = Path(sys.argv[2])
dst.parent.mkdir(parents=True, exist_ok=True)

im = Image.open(src).convert("RGBA")
w, h = im.size

# The source is a combined logo sheet. Select the first logo panel (StudyPilot AI).
# Use vertical panels for a tall sheet; otherwise use horizontal panels.
if h > w * 1.2:
    panel = im.crop((0, 0, w, h // 3))
else:
    panel = im.crop((0, 0, w // 3, h))

# Trim near-white/transparent outer canvas when present, retaining a little breathing room.
rgba = panel
alpha = rgba.getchannel("A")
rgb = rgba.convert("RGB")
import PIL.ImageChops as ImageChops
white = Image.new("RGB", rgb.size, (255, 255, 255))
diff = ImageChops.difference(rgb, white).convert("L")
mask = diff.point(lambda p: 255 if p > 18 else 0)
mask = ImageChops.lighter(mask, ImageChops.invert(alpha))
bbox = mask.getbbox()
if bbox:
    # Avoid trimming tiny details at the edge; pad the detected bounds by 4%.
    l, t, r, b = bbox
    px, py = max(2, int((r-l)*0.04)), max(2, int((b-t)*0.04))
    panel = panel.crop((max(0,l-px), max(0,t-py), min(panel.width,r+px), min(panel.height,b+py)))

# Center-fit the crop onto a transparent square so Android adaptive icons have safe margins.
square = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
logo = ImageOps.contain(panel, (410, 410), method=Image.Resampling.LANCZOS)
square.alpha_composite(logo, ((512-logo.width)//2, (512-logo.height)//2))
square.save(dst, format="PNG", optimize=True)
print(f"Created {dst} from source size {w}x{h}; selected panel {panel.size}")
