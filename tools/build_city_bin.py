from pathlib import Path
import struct


ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "data_kota"
OUT_FILE = ROOT / "app" / "src" / "main" / "assets" / "cities_falakpro.bin"


def dms_to_decimal(deg, minute, second, positive):
    value = float(deg) + float(minute) / 60.0 + float(second) / 3600.0
    return value if int(float(positive)) == 1 else -value


def write_string(out, value):
    data = value.encode("utf-8")
    out.write(struct.pack(">H", len(data)))
    out.write(data)


records = []
for txt in sorted(SOURCE_DIR.glob("*.txt")):
    if txt.name.lower() == "read me.txt":
        continue
    group = txt.stem
    with txt.open("r", encoding="utf-8-sig") as src:
        for line_no, raw in enumerate(src, 1):
            line = raw.strip()
            if not line:
                continue
            parts = [part.strip() for part in line.split(",")]
            if len(parts) != 16:
                raise ValueError(f"{txt.name}:{line_no} has {len(parts)} columns")

            name = parts[0]
            lat = dms_to_decimal(parts[1], parts[2], parts[3], parts[7])
            lon = dms_to_decimal(parts[4], parts[5], parts[6], parts[8])
            timezone = float(parts[9])
            elevation = float(parts[10])
            pressure = float(parts[12])
            temperature = float(parts[13])
            humidity = float(parts[14])
            lapse_rate = float(parts[15])
            records.append((name, group, lat, lon, elevation, timezone, pressure, temperature, humidity, lapse_rate))

OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
with OUT_FILE.open("wb") as out:
    out.write(b"FPKOTA1\0")
    out.write(struct.pack(">I", len(records)))
    for record in records:
        name, group, lat, lon, elevation, timezone, pressure, temperature, humidity, lapse_rate = record
        write_string(out, name)
        write_string(out, group)
        out.write(struct.pack(">dddddd", lat, lon, elevation, timezone, pressure, temperature))
        out.write(struct.pack(">dd", humidity, lapse_rate))

print(f"Wrote {len(records)} city records to {OUT_FILE}")
