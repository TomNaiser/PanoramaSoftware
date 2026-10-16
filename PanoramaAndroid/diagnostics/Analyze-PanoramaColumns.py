"""Offline statistics of Test-PanoramaColumns; never accesses a serial port."""
import argparse
import csv
import json
import statistics
from collections import defaultdict
from pathlib import Path


def read_csv(path):
    if not path.exists():
        return []
    with path.open(encoding="utf-8-sig", newline="") as source:
        return list(csv.DictReader(source))


def describe(values):
    values = sorted(float(value) for value in values if value is not None)
    if not values:
        return {"n": 0, "mean": None, "sd": None, "median": None, "p95": None, "min": None, "max": None}
    location = (len(values) - 1) * 0.95
    lower = int(location)
    upper = min(lower + 1, len(values) - 1)
    return {"n": len(values), "mean": statistics.mean(values),
            "sd": statistics.stdev(values) if len(values) > 1 else None,
            "median": statistics.median(values),
            "p95": values[lower] + (values[upper] - values[lower]) * (location - lower),
            "min": values[0], "max": values[-1]}


def write_csv(path, records):
    if not records:
        return
    with path.open("w", encoding="utf-8-sig", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=list(records[0]))
        writer.writeheader()
        writer.writerows(records)


def split_runs(rows):
    runs, active = [], []
    for row in rows:
        if row["enabled"] == 1:
            if active and row["targetMdeg"] != active[-1]["targetMdeg"]:
                runs.append(active)
                active = []
            active.append(row)
        elif active:
            runs.append(active)
            active = []
    if active:
        runs.append(active)
    return runs


def motion_metrics(run):
    first = run[0]
    sign = 1 if first["targetMdeg"] > first["tiltMdeg"] else -1
    cruise = [row for row in run if row["stage"] == 2]
    if not cruise:
        return {"actualDirection": "none", "steadySamples": 0, "steadyMeanDps": None,
                "peakFilteredDps": None, "startupLowSpeedMs": 0, "startupSpeedDip": False}
    steady = [row["velocityMdegPerSecond"] * sign / 1000 for row in cruise
              if row["sampleMs"] >= cruise[0]["sampleMs"] + 250
              and abs(row["targetMdeg"] - row["tiltMdeg"]) > 300]
    reached, weak_start, longest = False, None, 0
    for index, row in enumerate(run):
        progress = sign * (row["tiltMdeg"] - first["tiltMdeg"]) / 1000
        if row["stage"] not in (1, 2) or progress > 1:
            weak_start = None
            continue
        speed = row["velocityMdegPerSecond"] * sign / 1000
        reached = reached or speed >= 1.75
        if reached and row["stage"] == 2 and speed < 1:
            if weak_start is None:
                weak_start = row["sampleMs"]
            next_time = run[index + 1]["sampleMs"] if index + 1 < len(run) else row["sampleMs"]
            longest = max(longest, next_time - weak_start)
        else:
            weak_start = None
    return {"actualDirection": "up" if sign > 0 else "down", "steadySamples": len(steady),
            "steadyMeanDps": statistics.mean(steady) if len(steady) >= 5 else None,
            "peakFilteredDps": max(row["velocityMdegPerSecond"] * sign / 1000
                                   for row in run if row["stage"] in (1, 2)),
            "startupLowSpeedMs": longest, "startupSpeedDip": longest >= 100}


def failed_position(directory, point):
    records = [json.loads(line) for line in (directory / point["logName"]).read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    moves = [r for r in records if r["direction"] == "TX" and r["type"] == "MANUAL_MOVE"]
    statuses = [r for r in records if r["direction"] == "RX" and r["type"] == "STATUS"]
    if not moves or not statuses:
        return None
    move, status = moves[0], statuses[-1]
    # An unacknowledged request is not counted as a confirmed physical test.
    # It stays visible in the raw log and the separate attempt-abort list.
    accepted = any(r["direction"] == "RX" and r["type"] == "ACK" and r["fields"][1] == "MOVING"
                   and r["elapsedMs"] >= move["elapsedMs"] for r in records)
    if not accepted:
        return None
    diags = [r for r in records if r["type"] == "DIAGNOSTICS" and r["elapsedMs"] >= move["elapsedMs"]]
    faults = [r["fields"][13] for r in statuses if r["fields"][1] == "FAULT"]
    record = dict(point)
    record.update({"finalPanMdeg": status["fields"][9], "finalTiltMdeg": status["fields"][10],
                   "panErrorMdeg": str(int(status["fields"][9]) - int(point["panTargetMdeg"])),
                   "tiltErrorMdeg": str(int(status["fields"][10]) - int(point["tiltTargetMdeg"])),
                   "arrivalMs": "", "firstDiagnosticSequence": str(diags[0]["fields"][4]) if diags else "",
                   "crcErrors": str(sum(r["type"] == "CRC_BAD" for r in records)),
                   "nackFrames": str(sum(r["type"] == "NACK" for r in records)),
                   "stopConfirmed": str(any(r["type"] == "ACK" and r["fields"][1] == "STOPPED" for r in records)),
                   "sharedEndpoint": "False", "arrivalConfirmed": "False",
                   "pointFailure": faults[-1] if faults else "TEST_ABORTED"})
    return record


def analyze(directory, additional=()):
    plan = read_csv(directory / "plan.csv")
    positions, runs, trace_summaries, failures = [], [], [], []
    for source_directory in [directory, *additional]:
        part = read_csv(source_directory / "positions.csv")
        indices = {int(r["index"]) for r in part}
        for point in read_csv(source_directory / "plan.csv"):
            if int(point["index"]) not in indices and (source_directory / point["logName"]).exists():
                inferred = failed_position(source_directory, point)
                if inferred:
                    part.append(inferred)
        for record in part:
            record["pointFailure"] = record.get("pointFailure", "")
            record["arrivalConfirmed"] = record.get("arrivalConfirmed", str(bool(record["arrivalMs"])))
            record["sourceDirectory"] = str(source_directory)
        positions.extend(part)
        trace = [{key: int(value) for key, value in row.items()}
                 for row in read_csv(source_directory / "columns-trace.csv")]
        runs.extend(split_runs(trace))
        quality_path = source_directory / "columns-trace.summary.json"
        if quality_path.exists():
            trace_summaries.append(json.loads(quality_path.read_text(encoding="utf-8-sig")))
        failure_path = source_directory / "failed.json"
        if failure_path.exists():
            failures.append(json.loads(failure_path.read_text(encoding="utf-8-sig")))
    if len({int(p["index"]) for p in positions}) != len(positions):
        raise ValueError("Doppelte Positionsindizes: nur disjunkte Fortsetzungen zusammenfassen.")
    positions.sort(key=lambda p: int(p["index"]))
    enriched = []
    for position in positions:
        record = dict(position)
        for field in ("index", "repeat", "column", "row", "panTargetMdeg", "tiltTargetMdeg", "panErrorMdeg", "tiltErrorMdeg", "crcErrors", "nackFrames"):
            record[field] = int(record[field])
        record["arrivalMs"] = int(record["arrivalMs"]) if record["arrivalMs"] else None
        record["positionPassed"] = bool(record["arrivalMs"] is not None and not record["pointFailure"])
        log_records = [json.loads(line) for line in (Path(record["sourceDirectory"]) / record["logName"]).read_text(encoding="utf-8-sig").splitlines() if line.strip()]
        move = next((r for r in log_records if r["direction"] == "TX" and r["type"] == "MANUAL_MOVE"), None)
        sampled_motion = [r for r in log_records if move and r["direction"] == "RX" and r["type"] == "MOTION_DIAGNOSTICS" and r["elapsedMs"] >= move["elapsedMs"] and len(r["fields"]) >= 12]
        fine_samples = [r for r in sampled_motion if int(r["fields"][11]) == 5]
        record["finePhaseObserved"] = bool(fine_samples)
        record["fineTelemetrySamples"] = len(fine_samples)
        sequence = int(record["firstDiagnosticSequence"]) if record["firstDiagnosticSequence"] else None
        matches = [run for run in runs if sequence is not None and run[0]["sequence"] <= sequence <= run[-1]["sequence"]]
        record.update(motion_metrics(matches[0]) if len(matches) == 1 else {
            "actualDirection": "unmatched", "steadySamples": 0, "steadyMeanDps": None,
            "peakFilteredDps": None, "startupLowSpeedMs": None, "startupSpeedDip": None})
        record["panErrorDeg"] = record["panErrorMdeg"] / 1000
        record["tiltErrorDeg"] = record["tiltErrorMdeg"] / 1000
        enriched.append(record)
    write_csv(directory / "position-statistics.csv", enriched)
    row_steps = [record for record in enriched if record["kind"] == "row-step"]
    returns = [record for record in enriched if record["kind"] == "column-start"]
    grouped = defaultdict(list)
    for record in row_steps:
        grouped[(record["panTargetMdeg"], record["tiltTargetMdeg"])].append(record)
    cells = []
    for (pan, tilt), records in sorted(grouped.items()):
        error = describe(record["tiltErrorDeg"] for record in records)
        arrival = describe(record["arrivalMs"] for record in records)
        cells.append({"panDeg": pan / 1000, "tiltDeg": tilt / 1000, "n": len(records),
                      "failures": sum(not record["positionPassed"] for record in records),
                      "tiltErrorMeanDeg": error["mean"], "tiltErrorSdDeg": error["sd"],
                      "arrivalMeanMs": arrival["mean"], "arrivalSdMs": arrival["sd"],
                      "startupDips": sum(record["startupSpeedDip"] is True for record in records)})
    write_csv(directory / "statistics-by-position.csv", cells)
    completed_indices = {record["index"] for record in enriched}
    attempted_uncompleted = []
    for source_directory in [directory, *additional]:
        source_positions = {int(r["index"]) for r in read_csv(source_directory / "positions.csv")}
        for point in read_csv(source_directory / "plan.csv"):
            if int(point["index"]) not in source_positions and (source_directory / point["logName"]).exists() and failed_position(source_directory, point) is None:
                attempted_uncompleted.append(dict(point, sourceDirectory=str(source_directory)))
    result = {"plannedPositions": len(plan), "testedPositions": len(enriched),
              "completedPositions": sum(record["positionPassed"] for record in enriched),
              "failedPositions": sum(not record["positionPassed"] for record in enriched),
              "seriesComplete": {int(p["index"]) for p in plan} == {r["index"] for r in enriched},
              "rowSteps": len(row_steps), "columnStarts": len(returns),
              "attemptedUncompleted": attempted_uncompleted, "failures": failures, "traceQuality": trace_summaries,
              "crcErrors": sum(record["crcErrors"] for record in enriched),
              "nackFrames": sum(record["nackFrames"] for record in enriched),
              "rowTiltErrorDeg": describe(record["tiltErrorDeg"] for record in row_steps),
              "rowAbsoluteTiltErrorDeg": describe(abs(record["tiltErrorDeg"]) for record in row_steps),
              "rowPanErrorDeg": describe(record["panErrorDeg"] for record in row_steps),
              "rowArrivalMs": describe(record["arrivalMs"] for record in row_steps),
              "rowSteadyMeanDps": describe(record["steadyMeanDps"] for record in row_steps),
              "rowStartupDips": sum(record["startupSpeedDip"] is True for record in row_steps),
              "matchedRows": sum(record["actualDirection"] != "unmatched" for record in row_steps),
              "fineObservedPositions": sum(record["finePhaseObserved"] for record in enriched),
              "fineTelemetrySamples": sum(record["fineTelemetrySamples"] for record in enriched),
              "returnArrivalMs": describe(record["arrivalMs"] for record in returns),
              "returnStartupDips": sum(record["startupSpeedDip"] is True for record in returns)}
    (directory / "statistics.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    text = ["# Panorama-Spaltenstatistik", "",
            f"Getestete Positionen: **{len(enriched)} von {len(plan)}**; Zielruhe bestätigt: **{result['completedPositions']}**, "
            f"fehlgeschlagen: **{result['failedPositions']}**. Alle Positionen getestet: **{'Ja' if result['seriesComplete'] else 'Nein'}**.",
            "", "Spalten von +24° bis −20° in 2°-Schritten, Pan-Schritt 3°. Kamera nicht ausgelöst.", "",
            "| Messgröße, Zeilenschritte | n | Mittel | Standardabweichung | Median | P95 | Minimum | Maximum |",
            "|---|---:|---:|---:|---:|---:|---:|---:|"]
    for label, key in (("Tilt-Zielabweichung [°]", "rowTiltErrorDeg"), ("Absolute Tilt-Abweichung [°]", "rowAbsoluteTiltErrorDeg"),
                       ("Pan-Zielabweichung [°]", "rowPanErrorDeg"), ("Zeit bis Zielruhe [ms]", "rowArrivalMs"),
                       ("Fahrtfenster-Tempo [°/s]", "rowSteadyMeanDps")):
        item = result[key]
        format_value = lambda value: "—" if value is None else f"{value:.4f}"
        text.append("| " + label + " | " + str(item["n"]) + " | " + " | ".join(format_value(item[field]) for field in ("mean", "sd", "median", "p95", "min", "max")) + " |")
    dip_text = (f"Tempodelle nach erster Beschleunigung: {result['rowStartupDips']} bei {len(row_steps)} Zeilenschritten; "
                f"{result['returnStartupDips']} bei {len(returns)} Spaltenanfängen/Rückfahrten.") if result['matchedRows'] else "Keine zugeordneten USB-Reglerproben: Anlauf, Tempodellen und mechanische Geschwindigkeitsspitzen sind in dieser Reihe nicht bewertet."
    text.extend(["", dip_text, "",
                 f"Feinphase in abgefragter Bluetooth-Telemetrie bei {result['fineObservedPositions']} Positionen beobachtet, insgesamt {result['fineTelemetrySamples']} Proben. "
                 "Kurze Phasen können zwischen Abfragen liegen; die Zahl ist keine vollständige Übergangszählung.", "",
                 "Tempo: Mittel aus Fahrtproben ab 250 ms nach Phasenwechsel, mindestens fünf Proben, Zielrest >0,3°. "
                 "Tempodelle: gefiltertes Tempo nach Erreichen von 70 % des Solltempos im ersten Grad mindestens 100 ms unter 40 %. "
                 "Encoderrauschen kann einzelne negative Geschwindigkeiten erzeugen; die Auswertung behauptet keinen mechanischen Stillstand.", "",
                 "Standardabweichung ist die Stichprobenabweichung (n−1); P95 wird linear zwischen geordneten Werten interpoliert. "
                 "Winkelfehler schließen fehlgeschlagene Positionen ein; Ankunftszeiten enthalten nur bestätigte Ankünfte. "
                 "Die erreichte Abdeckung und n je Zelle sind maßgeblich; aus einer Messung je Zelle folgt keine Wiederholpräzision. "
                 "Spaltenanfänge und Rückfahrten werden getrennt von den 2°-Zeilenschritten ausgewertet. "
                 "Ankunftszeit wird ab Fahrbefehl bis zur ersten abgefragten bestätigten Zielruhe gemessen und enthält Kommunikationslatenz. "
                 "Bestätigte Ankunft verwendet die unveränderten Firmwarekriterien ohne zusätzliche Beruhigungszeit; "
                 "Bildschärfe und optisches Nachschwingen sind nicht geprüft."])
    for failure in failures:
        text.extend(["", "Protokollierter Abbruch eines Teilstücks: " + failure["message"]])
    text.extend(["", "## Einzelpositionen", "",
                 "Fehler sind letzte STATUS-Werte. Ein Zielabweichungsfehler wird aus dem internen Positionsmittel am Ruheübergang bestimmt; "
                 "ein späterer Einzelwert kann wieder innerhalb der Toleranz liegen. Der Fehlerstatus bleibt maßgeblich.", "",
                 "| Punkt | Pan-Ziel [°] | Tilt-Ziel [°] | Ergebnis | Tilt-Fehler [°] | Pan-Fehler [°] | Ankunft [s] |",
                 "|---:|---:|---:|---|---:|---:|---:|"])
    for record in enriched:
        label = "Zielruhe" if record["positionPassed"] else record["pointFailure"]
        arrival = "—" if record["arrivalMs"] is None else f"{record['arrivalMs']/1000:.3f}"
        log_path = (Path(record["sourceDirectory"]) / record["logName"]).as_posix()
        text.append(f"| [{record['index']}]({log_path}) | {record['panTargetMdeg']/1000:.3f} | "
                    f"{record['tiltTargetMdeg']/1000:.1f} | {label} | {record['tiltErrorDeg']:+.3f} | "
                    f"{record['panErrorDeg']:+.3f} | {arrival} |")
    (directory / "STATISTIK.md").write_text("\n".join(text) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    parser.add_argument("additional", type=Path, nargs="*", help="Disjunkte Fortsetzungen mit gleichem unverändertem Firmwarestand")
    arguments = parser.parse_args()
    analyze(arguments.directory.resolve(), [path.resolve() for path in arguments.additional])
