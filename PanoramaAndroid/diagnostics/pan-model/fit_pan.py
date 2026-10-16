"""Identify a hybrid PWM/inertia/friction model from verified RAM traces.

Fit entire 10-degree trajectories; reserve all short moves for validation.
No device access, parameter writes or firmware updates occur in this tool.
"""
import argparse
import csv
import json
import math
import os
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE / '.deps'))
os.environ.setdefault('MPLCONFIGDIR', str(HERE / '.mpl-cache'))
import numpy as np
from scipy.optimize import differential_evolution, minimize
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

NAMES = ['driveAccelerationPerPwm', 'poweredDampingPerSecond',
         'runningFrictionPositivePwm', 'runningFrictionNegativePwm',
         'staticFrictionPositivePwm', 'staticFrictionNegativePwm',
         'coastCoulombDecelerationDps2', 'coastDampingPerSecond']
BOUNDS = [(0.1, 30), (0.05, 20), (10, 42), (10, 42), (20, 52), (20, 52), (5, 180), (0.01, 15)]


def read_case(path):
    summary = json.loads(path.with_suffix('.summary.json').read_text(encoding='utf-8-sig'))
    if (not summary['integrityVerified'] or summary['overwritten'] or summary['badTiming']
            or summary['invalidSensorSamples'] or summary['maximumSampleGapMs'] > 30):
        raise ValueError(f'Unusable trace quality: {path}')
    with path.open(encoding='utf-8-sig', newline='') as file:
        rows = list(csv.DictReader(file))
    time = np.array([float(r['sampleMs']) / 1000 for r in rows])
    pan = np.array([float(r['panMdeg']) / 1000 for r in rows])
    pwm = np.array([float(r['panAppliedPwm']) for r in rows])
    # The initial low-PWM buildup is stationary on this measured rig. Use
    # its first 200 ms to estimate encoder zero without differentiating noise.
    zero = float(np.median(pan[time <= time[0] + .20]))
    direction = 1 if pwm.sum() >= 0 else -1
    frames = [json.loads(line) for line in path.with_suffix('.jsonl').read_text().splitlines() if line.strip()]
    if any(r['type'] in ('CRC_BAD', 'NACK') for r in frames):
        raise ValueError(f'Transport error: {path}')
    move = next(r for r in frames if r['direction'] == 'TX' and r['type'] == 'MANUAL_MOVE')
    target = int(move['fields'][0]) / 1000
    return {'name': path.stem, 'time': time-time[0], 'delta': np.diff(time),
            'position': direction*(pan-zero), 'pwm': direction*pwm, 'direction': direction,
            'target': direction*(target-zero), 'zero': zero, 'tiltDeg': summary['tiltMdeg']/1000,
            'training': '-long-' in path.stem, 'rawEndpointErrorDeg': float(np.median(pan[-10:])-target),
            'targetMiss': any(r['type'] == 'STATUS' and r['fields'][1] == 'FAULT' for r in frames)}


def simulate(parameters, case):
    """Exact constant-input integration between timestamped PWM updates.

    Powered: theta'' = A*(u-F_run*sign(v)) - B*v.
    Freewheel: theta'' = -C_coast*sign(v) - B_coast*v.
    At rest, static friction locks motion until u reaches F_static.
    A combines motor torque gain, gearing and inverse equivalent inertia;
    these cannot be identified separately from angle and PWM alone.
    """
    drive, damping, fp, fn, sp, sn, coast, coast_damping = parameters
    friction = fp if case['direction'] > 0 else fn
    static = sp if case['direction'] > 0 else sn
    position = velocity = 0.0
    result = np.zeros(len(case['time']))
    for i, dt in enumerate(case['delta'], 1):
        u = max(0., float(case['pwm'][i-1]))
        if velocity < .02 and u > 0 and u < static:
            velocity = 0.
        else:
            a, b = (drive*(u-friction), damping) if u else (-coast, coast_damping)
            equilibrium = a/b
            decay = math.exp(-b*dt)
            next_velocity = equilibrium+(velocity-equilibrium)*decay
            if next_velocity < 0:
                stop_time = min(dt, math.log((velocity-equilibrium)/(-equilibrium))/b) if velocity > 0 else 0.
                position += equilibrium*stop_time+(velocity-equilibrium)*(1-math.exp(-b*stop_time))/b
                velocity = 0.
            else:
                position += equilibrium*dt+(velocity-equilibrium)*(1-decay)/b
                velocity = next_velocity
        result[i] = position
    return result


def huber(residual, width=.06):
    absolute = np.abs(residual)
    return np.where(absolute <= width, .5*residual**2, width*(absolute-.5*width))


def fit(cases, iterations):
    training = [c for c in cases if c['training']]
    if len(training) < 2 or {c['direction'] for c in training} != {-1, 1}:
        raise ValueError('Need powered/coast training trajectories in both directions')

    def loss(p):
        if p[4] < p[2] or p[5] < p[3]:
            return 100 + max(0, p[2]-p[4])**2 + max(0, p[3]-p[5])**2
        total = 0.
        for case in training:
            prediction = simulate(p, case)
            residual = prediction-case['position']
            total += .7*float(huber(residual).mean())+.3*float(huber(np.array([np.median(residual[-10:])]))[0])
        return total/len(training)

    coarse = differential_evolution(loss, BOUNDS, seed=20261016, maxiter=iterations, popsize=6,
                                    tol=.002, polish=False, workers=1)
    refined = minimize(loss, coarse.x, method='Powell', bounds=BOUNDS,
                       options={'maxiter':60, 'maxfev':1800, 'xtol':.0005, 'ftol':.0001})
    best = refined if refined.fun < coarse.fun else coarse
    return best.x, {'trainingLoss': float(best.fun), 'evaluations': int(coarse.nfev+refined.nfev),
                    'seed':20261016, 'trainingCases':[c['name'] for c in training]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--data', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--iterations', type=int, default=45)
    parser.add_argument('--parameters', type=Path, help='Validate a previously fitted model without fitting it again')
    args = parser.parse_args()
    cases, excluded = [], []
    for path in sorted(args.data.glob('buffered-*.csv')):
        try:
            cases.append(read_case(path))
        except ValueError as error:
            excluded.append({'file':path.name, 'reason':str(error)})
    if args.parameters:
        previous = json.loads(args.parameters.read_text())
        parameters = np.array([previous['parameters'][name] for name in NAMES])
        fitting = previous['fitting']
    else:
        parameters, fitting = fit(cases, args.iterations)
    args.output.mkdir(parents=True, exist_ok=True)
    metrics = []
    figure, axes = plt.subplots(math.ceil(len(cases)/2), 2, figsize=(12, 3.1*math.ceil(len(cases)/2)), squeeze=False)
    for axis, case in zip(axes.flat, cases):
        prediction = simulate(parameters, case)
        residual = prediction-case['position']
        metric = {'name':case['name'], 'training':case['training'], 'tiltDeg':case['tiltDeg'],
                  'rmsPredictionErrorDeg':float(np.sqrt(np.mean(residual**2))),
                  'maximumPredictionErrorDeg':float(np.max(np.abs(residual))),
                  'endpointPredictionErrorDeg':float(np.median(residual[-10:])),
                  'observedEndpointErrorDeg':case['rawEndpointErrorDeg'], 'targetMiss':case['targetMiss']}
        metrics.append(metric)
        axis.plot(case['time'], case['position'], color='#26364a', label='Messung', lw=1.3)
        axis.plot(case['time'], prediction, color='#df7137', label='Modell', lw=1.4)
        axis.axhline(case['target'], color='#4484b7', linestyle='--', lw=.8, label='Ziel')
        axis.set(title=case['name'].replace('buffered-', '')+(' / Anpassung' if case['training'] else ' / Prüfung'),
                 xlabel='Zeit [s]', ylabel='Weg in Fahrtrichtung [°]')
        axis.grid(alpha=.22);axis.legend(fontsize=8)
    for axis in list(axes.flat)[len(cases):]: axis.set_visible(False)
    figure.tight_layout();figure.savefig(args.output/'model-vs-measurement.png', dpi=160);plt.close(figure)
    validation = [m for m in metrics if not m['training']]
    worst_end = max((abs(m['endpointPredictionErrorDeg']) for m in validation), default=None)
    accepted = len(validation)>=4 and worst_end<=.10 and max(m['rmsPredictionErrorDeg'] for m in validation)<=.10
    report = {'model':'hybrid-powered-freewheel-coulomb-static-friction', 'units':'degrees, seconds, signed PWM counts',
              'parameters':dict(zip(NAMES, map(float, parameters))), 'fitting':fitting, 'metrics':metrics,
              'excludedIncompleteOrCorruptTraces':excluded,
              'validationCaseCount':len(validation), 'maximumHeldOutEndpointPredictionErrorDeg':worst_end,
              'acceptedForControllerTuning':accepted,
              'acceptanceCriterion':'At least four entire held-out short moves; endpoint and RMS prediction error <=0.10 deg',
              'scope':'Measured payload and angle range; observed extremes are not a proven physical worst-case bound.',
              'unidentified':'Motor torque constant, inertia and gearbox gain separately; backlash/compliance beyond residual evidence.'}
    (args.output/'model.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
