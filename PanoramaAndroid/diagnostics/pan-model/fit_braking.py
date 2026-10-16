"""Causal stop-distance model: distinguish filter lag from passive coast.

All features use samples at/before power removal. Fit long trajectories only,
choose filter length by leave-one-long-move-out; validate on short moves.
"""
import argparse
import json
import math
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent/'.deps'))
import numpy as np
from scipy.optimize import linprog
from fit_pan import read_case, HERE
import matplotlib.pyplot as plt


def features(case, count):
    t, p, u = case['time'], case['position'], case['pwm']
    active = np.flatnonzero(u>0)
    if not len(active) or active[-1]+1>=len(u):
        raise ValueError('No recorded power-removal transition')
    cut=int(active[-1]+1)
    if cut<count: raise ValueError('Insufficient causal history')
    x=t[cut-count+1:cut+1]-t[cut]
    coefficients=np.polynomial.polynomial.polyfit(x,p[cut-count+1:cut+1],2)
    current_position,current_velocity,acceleration=coefficients[0],max(0.,coefficients[1]),2*coefficients[2]
    recent_position=float(np.mean(p[cut-2:cut+1]))
    stop_position=float(np.median(p[-10:]))
    filtered=0.
    for i in range(1,cut+1):
        start=max(0,i-4)
        measured=np.polyfit(t[start:i+1]-t[i],p[start:i+1],1)[0]
        dt=t[i]-t[i-1]
        filtered+=dt/(.03+dt)*(measured-filtered)
    start=max(0,cut-6)
    window=np.polyfit(t[start:cut+1]-t[cut],p[start:cut+1],1)[0]
    old_velocity=max(0.,window,filtered)
    position_lag=current_position-recent_position
    return {'name':case['name'],'training':case['training'],'window':count,
            'cutTime':float(t[cut]),'velocityNow':float(current_velocity),'accelerationNow':float(acceleration),
            'oldVelocity':float(old_velocity),'positionLag':float(position_lag),
            'measuredAdvance':float(stop_position-recent_position),
            'x':[float(current_velocity**2),float(current_velocity)],
            'y':float(stop_position-recent_position-position_lag)}


def minimax(rows):
    x=np.array([r['x'] for r in rows]);y=np.array([r['y'] for r in rows])
    a=np.vstack([np.column_stack([x,-np.ones(len(x))]),np.column_stack([-x,-np.ones(len(x))])])
    result=linprog([0.,0.,1.],A_ub=a,b_ub=np.concatenate([y,-y]),
                   bounds=[(1/400,1/20),(0.,.10),(0.,None)],method='highs')
    if not result.success: raise ValueError(result.message)
    return result.x[:2]


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--data',type=Path,required=True);parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--parameters',type=Path,help='Validate frozen model on entirely new moves; never fit these data')
    args=parser.parse_args();cases=[];excluded=[]
    for path in sorted(args.data.glob('buffered-*.csv')):
        try: cases.append(read_case(path))
        except ValueError as error: excluded.append({'file':path.name,'reason':str(error)})
    if args.parameters:
        frozen=json.loads(args.parameters.read_text());count=frozen['windowSamples']
        parameters=np.array([1/(2*frozen['effectiveCoastDecelerationDps2']),frozen['additionalDriveOffDelaySeconds']])
        score=frozen['leaveOneTrainingMoveOutWorstErrorDeg'];rows=[features(c,count) for c in cases]
        for row in rows:row['training']=False
    else:
        choices=[]
        for count in [5,7,9,11,13,15]:
            rows=[features(c,count) for c in cases];training=[r for r in rows if r['training']]
            errors=[]
            for held in training:
                params=minimax([r for r in training if r is not held])
                errors.append(abs(float(np.dot(held['x'],params))-held['y']))
            choices.append((max(errors),count,rows))
        score,count,rows=min(choices,key=lambda c:c[0]);parameters=minimax([r for r in rows if r['training']])
    for row in rows:
        row['oldPrediction']=row['oldVelocity']**2/120
        row['newPrediction']=row['positionLag']+float(np.dot(row['x'],parameters))
        row['oldError']=row['oldPrediction']-row['measuredAdvance']
        row['newError']=row['newPrediction']-row['measuredAdvance']
    held=[r for r in rows if not r['training']]
    old=max(abs(r['oldError']) for r in held);new=max(abs(r['newError']) for r in held)
    report={'model':'quadratic causal velocity + position-filter lag + passive constant-deceleration/delay',
            'windowSamples':count,'effectiveCoastDecelerationDps2':float(1/(2*parameters[0])),
            'additionalDriveOffDelaySeconds':float(parameters[1]),'leaveOneTrainingMoveOutWorstErrorDeg':float(score),
            'heldOutOldWorstPredictionErrorDeg':float(old),'heldOutNewWorstPredictionErrorDeg':float(new),
            'acceptedForControllerExperiment':len(held)>=4 and new<=.10 and score<=.15,
            'excluded':excluded,'rows':rows}
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/'braking-model.json').write_text(json.dumps(report,indent=2)+'\n')
    labels=[r['name'].replace('buffered-','').replace('-retry','') for r in rows];x=np.arange(len(rows))
    fig,ax=plt.subplots(figsize=(12,5))
    ax.bar(x-.18,[r['oldError'] for r in rows],.36,label='Bisherige Vorhersage',color='#a45147')
    ax.bar(x+.18,[r['newError'] for r in rows],.36,label='Physikalische Korrektur',color='#3c7c86')
    ax.axhline(.10,color='#999',ls='--',lw=.8);ax.axhline(-.10,color='#999',ls='--',lw=.8)
    ax.set(xticks=x,xticklabels=labels,ylabel='Vorhersagefehler des Stillstandsorts [°]')
    ax.tick_params(axis='x',labelrotation=25);ax.legend();ax.grid(axis='y',alpha=.2)
    fig.tight_layout();fig.savefig(args.output/'braking-prediction.png',dpi=170);plt.close(fig)
    print(json.dumps({k:v for k,v in report.items() if k not in ['rows','excluded']},indent=2))


if __name__=='__main__':main()
