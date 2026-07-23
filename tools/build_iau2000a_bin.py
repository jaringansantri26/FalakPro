from pathlib import Path
import re
import struct


ROOT = Path(__file__).resolve().parents[1]
SOURCE_FILE = ROOT / "IAU2000A.txt"
OUT_FILE = ROOT / "app" / "src" / "main" / "assets" / "iau2000a_nutation.bin"

DATA_LINE = re.compile(r"^\s*\d+\s+")
MAGIC = b"FPNUT2A\0"


records = []
with SOURCE_FILE.open("r", encoding="utf-8-sig") as src:
    for line_no, raw in enumerate(src, 1):
        if not DATA_LINE.match(raw):
            continue
        parts = raw.split()
        if len(parts) != 21:
            raise ValueError(f"{SOURCE_FILE.name}:{line_no} has {len(parts)} columns")

        multipliers = [int(value) for value in parts[1:15]]
        coeffs = [float(value) for value in parts[15:21]]
        if any(value < -128 or value > 127 for value in multipliers):
            raise ValueError(f"{SOURCE_FILE.name}:{line_no} multiplier outside int8 range")
        records.append((multipliers, coeffs))

if len(records) != 1365:
    raise ValueError(f"Expected 1365 IAU2000A terms, got {len(records)}")

OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
with OUT_FILE.open("wb") as out:
    out.write(MAGIC)
    out.write(struct.pack(">I", len(records)))
    for multipliers, coeffs in records:
        out.write(struct.pack(">14b", *multipliers))
        out.write(struct.pack(">6d", *coeffs))

print(f"Wrote {len(records)} IAU 2000A nutation terms to {OUT_FILE}")
