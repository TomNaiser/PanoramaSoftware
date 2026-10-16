"""Assess the interior cruise of one native MANUAL_MOVE trace, not only arrival.

Start and endpoint braking are deliberately excluded. This is a coarse
Bluetooth sample check; it cannot certify optical smoothness between samples.
"""
import argparse
import json
import math
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("trace", type=Path)
parser.add_argument("--check", action="store_true")
parser.add_argument("--maximum-cv", type=float, default=0.45)
args = parser.parse_args()
records = [json.loads(line) for line in args.trace.read_text(encoding="utf-8-sig").splitlines() if line.strip()]
move = next(r for r in records if r["type"] == "MANUAL_MOVE" and r["direction"] == "TX")
wire = next(r for r in records if r["type"] == "WIRE" and "|MANUAL_MOVE|" in r["fields"][0])
sequence = wire["fields"][0].split("|")[1]
reached = next((r for r in records if r["type"] == "MANUAL_STATUS" and r["fields"][:3] == [sequence, "MOVE", "REACHED"]), None)
origin = next(int(r["fields"][9]) for r in records if r["type"] == "STATUS")
direction = 1 if int(move["fields"][0]) > origin else -1
latest = None
samples = []
for r in records:
    if r["elapsedMs"] < move["elapsedMs"]:
        continue
    if r["type"] == "DIAGNOSTICS":
        latest = r
    if r["type"] != "MOTION_DIAGNOSTICS" or latest is None:
        continue
    f = r["fields"]
    age = r["elapsedMs"] - move["elapsedMs"]
    if age < 1500 or f[10] != "2" or abs(int(latest["fields"][11])) <= 2000:
        continue
    samples.append({"elapsedMs": age, "velocityDps": direction * int(f[2]) / 1000,
                    "pwm": abs(int(f[6])), "positionDeg": int(latest["fields"][9]) / 1000})
speeds = [s["velocityDps"] for s in samples]
mean = sum(speeds) / len(speeds) if speeds else 0
cv = math.sqrt(sum((v - mean) ** 2 for v in speeds) / len(speeds)) / mean if speeds and mean else None
summary = {"trace": str(args.trace), "sampleCount": len(samples), "meanDps": mean,
           "minimumDps": min(speeds) if speeds else None, "maximumDps": max(speeds) if speeds else None,
           "coefficientOfVariation": cv, "durationMs": int(reached["fields"][5]) if reached else None,
           "reached": reached is not None, "invalidCrc": sum(r["type"] == "CRC_BAD" for r in records),
           "faultFrames": sum(r["type"] == "STATUS" and r["fields"][1] == "FAULT" for r in records),
           "samples": samples}
print(json.dumps(summary, ensure_ascii=False, indent=2))
if args.check and (not summary["reached"] or summary["invalidCrc"] or summary["faultFrames"] or len(samples) < 5 or cv is None or cv > args.maximum_cv):
    raise SystemExit("Pan-Fahrt ohne bestaetigte ruhige Geschwindigkeitsphase: Messgrenze ueberschritten oder zu wenig Daten")
