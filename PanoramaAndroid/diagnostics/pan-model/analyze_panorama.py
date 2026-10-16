"""Offline evaluation of a copied app archive; never connects to hardware.

Reconstructed raster targets are explicitly excluded from encoder statistics.
Support STATUS is sampled every five seconds, so warning counts are lower bounds.
"""
import argparse
import csv
from datetime import datetime
import json
from pathlib import Path
import sqlite3
import statistics
import sys
import tarfile

sys.path.insert(0, str(Path(__file__).resolve().parent / '.deps'))
from fit_pan import HERE  # sets a local Matplotlib cache and a noninteractive backend
import matplotlib.pyplot as plt


def describe(values):
    ordered = sorted(abs(v) for v in values)
    q = (len(ordered) - 1) * .95
    lo = int(q)
    p95 = ordered[lo] + (q - lo) * (ordered[min(lo + 1, len(ordered) - 1)] - ordered[lo])
    return dict(count=len(values), mean=statistics.mean(values),
                standardDeviation=statistics.stdev(values) if len(values) > 1 else 0.,
                minimum=min(values), maximum=max(values),
                maximumAbsolute=max(ordered), p95Absolute=p95)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--run-id', required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    snapshot = args.output / 'app-snapshot'
    snapshot.mkdir(exist_ok=True)
    logs = []
    with tarfile.open(args.archive) as archive:
        for member in archive.getmembers():
            if not member.isfile():
                continue
            name = member.name.removeprefix('./')
            if name in ('databases/captures.db', 'databases/captures.db-wal'):
                (snapshot / Path(name).name).write_bytes(archive.extractfile(member).read())
            elif name in ('files/support.log', 'files/support.previous.log'):
                data = archive.extractfile(member).read()
                (args.output / Path(name).name).write_bytes(data)
                logs.extend(data.decode('utf-8').splitlines())
    # Only the local copy is opened. No stale on-device SHM file is reused.
    with sqlite3.connect(snapshot / 'captures.db') as db:
        db.row_factory = sqlite3.Row
        run = db.execute('SELECT * FROM runs WHERE run_id=? ORDER BY stamp DESC LIMIT 1',
                         (args.run_id,)).fetchone()
        assert run is not None, 'Run missing from copied database'
        raw = db.execute('SELECT ordinal,payload,logged FROM captures WHERE run=? AND manual=0 ORDER BY ordinal',
                         (run['id'],)).fetchall()
        assert [r['ordinal'] for r in raw] == list(range(1, 281)), 'Expected all 280 unique ordinals'
        assert run['completed'] == 280, 'Firmware did not report 280 completed images'
        original = db.execute('SELECT COUNT(*) FROM captures WHERE run IN (SELECT id FROM runs WHERE run_id=?) AND manual=0',
                              ('1927212438',)).fetchone()[0]
        plan = str(run['plan'])
    measured, reconstructed = [], []
    for item in raw:
        p = item['payload'].split('|')
        assert p[0] == 'IMAGE_COMPLETED' and len(p) == 13 and p[2] == args.run_id
        f = p[1:]
        if f[11] != '0':
            reconstructed.append(item['ordinal'])
            continue
        row = dict(capture=int(f[2]), column=int(f[5]) + 1, row=int(f[6]) + 1,
                   panTargetDeg=int(f[7]) / 1000., tiltTargetDeg=int(f[8]) / 1000.,
                   panErrorDeg=(int(f[9]) - int(f[7])) / 1000.,
                   tiltErrorDeg=(int(f[10]) - int(f[8])) / 1000., loggedUtc=item['logged'])
        measured.append(row)
    assert len(measured) == 280 and not reconstructed, 'Live measurements incomplete; do not claim reconstructed targets as encoder readings'
    with (args.output / 'capture-endpoints.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(measured[0]))
        writer.writeheader(); writer.writerows(measured)
    with args.baseline.open(encoding='utf-8-sig', newline='') as stream:
        baseline = [{k: float(v) for k, v in r.items()} for r in csv.DictReader(stream)]
    columns = []
    for column in range(1, 36):
        selected = [r for r in measured if r['column'] == column]
        assert len(selected) == 8 and [r['row'] for r in selected] == list(range(1, 9))
        columns.append(dict(column=column, panErrorDeg=describe([r['panErrorDeg'] for r in selected])))
    times = [datetime.fromisoformat(r['loggedUtc'].replace('Z', '+00:00')) for r in measured]
    within, returns = [], []
    for i in range(1, len(measured)):
        delta = (times[i] - times[i - 1]).total_seconds()
        (within if measured[i]['column'] == measured[i-1]['column'] else returns).append(delta)
    relevant = []
    active = False
    for line in sorted(logs):
        if f'TEST_MODE REAL_MODEL_PANORAMA_35X8 run={args.run_id}' in line:
            active = True
        if active:
            relevant.append(line)
        if f'TEST_END REAL_MODEL_PANORAMA_35X8 stopped-and-cleared run={args.run_id}' in line:
            break
    assert relevant and 'TEST_END' in relevant[-1], 'Completed cleanup missing from support archive'
    (args.output / 'run-support.log').write_text('\n'.join(relevant) + '\n', encoding='utf-8')
    statuses = [line.split(' STATUS ', 1)[1].split('|') for line in relevant if ' STATUS ' in line]
    timeouts = sorted({int(f[5]) * 8 + int(f[6]) for f in statuses
                       if f[0] == args.run_id and f[13] == 'STEP_MOVE_TIMEOUT'})
    errors = [line for line in relevant if '|NACK|' in line or 'INVALID_CRC' in line or '|FAULT|' in line]
    summary = dict(runId=args.run_id, firmware='2.4.67', configRevision=22,
                   plan=plan, completed=True, measuredCaptures=len(measured), reconstructedCaptures=reconstructed,
                   independentPanColumns=35, originalUserCapturesRetained=original,
                   errors=errors, sampledTimeoutStepsZeroBasedLowerBound=timeouts,
                   timeoutCountIsExact=False,
                   warningCoverage='Support STATUS sampled every five seconds; absence/count is not a complete per-step warning record.',
                   panErrorDeg=describe([r['panErrorDeg'] for r in measured]),
                   tiltErrorDeg=describe([r['tiltErrorDeg'] for r in measured]),
                   firstSixColumnsPanErrorDeg=describe([r['panErrorDeg'] for r in measured[:48]]),
                   baselinePanErrorDeg=describe([r['panErrorDeg'] for r in baseline]),
                   baselineTiltErrorDeg=describe([r['tiltErrorDeg'] for r in baseline]),
                   firstToLastCaptureSeconds=(times[-1]-times[0]).total_seconds(),
                   withinColumnCaptureIntervalSeconds=describe(within),
                   columnReturnCaptureIntervalSeconds=describe(returns),
                   intervalNote='Intervals include settling, triggering and exposure wait; return intervals include the long Tilt return. Not pure motor movement duration.',
                   columns=columns)
    (args.output / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    fig, axes = plt.subplots(2, 1, figsize=(11, 7), constrained_layout=True)
    means = [c['panErrorDeg']['mean'] for c in columns]
    axes[0].errorbar(range(1, 36), means,
                    yerr=[[m-c['panErrorDeg']['minimum'] for m,c in zip(means, columns)],
                          [c['panErrorDeg']['maximum']-m for m,c in zip(means, columns)]],
                    fmt='o-', capsize=3, label='New model: mean and measured min/max per column')
    axes[0].plot(range(1, 7), [statistics.mean(r['panErrorDeg'] for r in baseline if r['column'] == c) for c in range(1, 7)],
                 's--', label='Baseline: mean per column')
    for y in (-.3, .3): axes[0].axhline(y, color='gray', linestyle=':', linewidth=1)
    axes[0].set(xlabel='Column (8 photos share a Pan arrival)', ylabel='Pan error [degree]')
    axes[0].legend(); axes[0].grid(alpha=.25)
    axes[1].plot([r['capture'] for r in measured], [r['tiltErrorDeg'] for r in measured], '.', label='New model run')
    axes[1].plot([r['capture'] for r in baseline], [r['tiltErrorDeg'] for r in baseline], 'x', label='Baseline run')
    axes[1].set(xlabel='Capture ordinal', ylabel='Tilt error [degree]')
    axes[1].legend(); axes[1].grid(alpha=.25)
    fig.suptitle('Fixed panorama recipe: encoder snapshot errors at shutter completion')
    fig.savefig(args.output / 'panorama-comparison.png', dpi=160)
    print(json.dumps({k: v for k, v in summary.items() if k != 'columns'}, indent=2))


if __name__ == '__main__':
    main()
