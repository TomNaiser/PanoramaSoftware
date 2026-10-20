package de.tomnaiser.panoramaandroid;

/** A client-side quality pause is independent of firmware fault and warning thresholds. */
final class CaptureQualityGuard {
    private long run=-1,ordinal=-1;
    static final class Issue {
        final long run,ordinal,index,panError,tiltError;
        Issue(String[] f) {
            run=Long.parseLong(f[1]);ordinal=Long.parseLong(f[2]);index=Long.parseLong(f[4]);
            panError=Math.abs(Long.parseLong(f[9])-Long.parseLong(f[7]));
            tiltError=Math.abs(Long.parseLong(f[10])-Long.parseLong(f[8]));
        }
        String describe() {
            return String.format(java.util.Locale.GERMANY,"Aufnahme %d: Zielabweichung Pan %.3f° / Tilt %.3f°. ",ordinal,panError/1000.0,tiltError/1000.0);
        }
    }
    Issue accept(String[] f) {
        if(f.length!=12 || f[11].equals("1")) return null;
        Issue issue=new Issue(f);
        if(issue.run==run && issue.ordinal<=ordinal) return null;
        run=issue.run;ordinal=issue.ordinal;
        return issue.panError>1000 || issue.tiltError>1000 ? issue : null;
    }
}
