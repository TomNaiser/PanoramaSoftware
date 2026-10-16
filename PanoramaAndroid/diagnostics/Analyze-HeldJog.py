"""Summarize bounded P2 held-jog logs; positions come from STATUS, not references."""
import argparse
import json
from pathlib import Path
from statistics import median


def summarize(path):
    rows = [json.loads(line) for line in path.read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    starts = [r for r in rows if r["type"] in ("MANUAL_JOG_START", "MANUAL_JOG_START_VELOCITY") and r["direction"] == "TX"]
    if len(starts) != 1:
        raise ValueError(f"{path}: expected one gesture")
    axis, direction = map(int, starts[0]["fields"][1:3])
    acknowledgements = [r for r in rows if r["type"] == "ACK" and r["fields"][1] == "JOGGING"]
    if not acknowledgements:
        raise ValueError(f"{path}: no start acknowledgement")
    sequence = acknowledgements[0]["fields"][0]
    manual = [r for r in rows if r["type"] == "MANUAL_STATUS" and r["fields"][0] == sequence]
    active = next(r for r in manual if r["fields"][2] == "ACTIVE")
    terminal = next(r for r in manual if r["fields"][2] != "ACTIVE")
    statuses = [r for r in rows if r["type"] == "STATUS" and r["elapsedMs"] >= active["elapsedMs"]]
    motion = [r for r in rows if r["type"] == "MOTION_DIAGNOSTICS" and
              active["elapsedMs"] <= r["elapsedMs"] <= terminal["elapsedMs"]]
    origin = int(active["fields"][3 + axis]) / 1000
    final = median(int(r["fields"][9 + axis]) / 1000 for r in statuses[-3:])
    faults = sorted({r["fields"][13] for r in statuses if r["fields"][1] == "FAULT"})
    duration = int(terminal["fields"][5])
    requested = int(starts[0]["fields"][3]) / 1000 if starts[0]["type"] == "MANUAL_JOG_START_VELOCITY" else None
    position=origin; onset=None; samples=[]
    for row in rows:
        if row["type"] == "DIAGNOSTICS": position=int(row["fields"][9+axis])/1000
        if row["type"] != "MOTION_DIAGNOSTICS" or not active["elapsedMs"] <= row["elapsedMs"] <= terminal["elapsedMs"]: continue
        if onset is None and (position-origin)*direction>=0.3: onset=row["elapsedMs"]
        if onset is not None and int(row["fields"][6+axis])*direction>0:
            samples.append((row["elapsedMs"],int(row["fields"][2+axis])*direction/1000,abs(int(row["fields"][4+axis]))/1000))
    longest=0; slow_start=None
    for time,velocity,reference in samples:
        if velocity<min(0.5,reference*0.25):
            if slow_start is None: slow_start=time
        elif slow_start is not None:
            longest=max(longest,time-slow_start); slow_start=None
    if slow_start is not None: longest=max(longest,samples[-1][0]-slow_start)
    steady=[v for _,v,reference in samples if requested and reference>=requested*0.95]
    return {
        "log": path.name,
        "firmware": next(r["fields"][0] for r in rows if r["type"] == "DEVICE_INFO"),
        "axis": "Tilt" if axis else "Pan", "direction": direction,
        "durationMs": duration, "result": terminal["fields"][2],
        "startDeg": origin, "finalDeg": round(final, 3),
        "directedDistanceIncludingCoastDeg": round((final - origin) * direction, 3),
        "sampledVelocityPeakDps": round(max(abs(int(r["fields"][2 + axis])) / 1000 for r in motion), 3),
        "maximumPwm": max(abs(int(r["fields"][6 + axis])) for r in motion),
        "requestedVelocityDps": requested,
        "steadySamples": len(steady),
        "steadyMeanDps": round(sum(steady)/len(steady),3) if steady else None,
        "longestSampledSlowEpisodeMs": longest,
        "faults": faults, "discardedCrcFrames": sum(r["type"] == "CRC_BAD" for r in rows),
        "finalPwm": [int(rows_i["fields"][i]) for i in (6, 7)
                     for rows_i in [next(r for r in reversed(rows) if r["type"] == "MOTION_DIAGNOSTICS")]],
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logs", nargs="+", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = [summarize(path) for path in args.logs]
    rendered = json.dumps(result, indent=2, ensure_ascii=False) + "\n"
    if args.output:
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered)
