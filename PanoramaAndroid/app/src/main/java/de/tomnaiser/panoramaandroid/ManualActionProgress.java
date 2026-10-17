package de.tomnaiser.panoramaandroid;

/** Completion belongs to the ACKed command, never to proximity or old log events. */
final class ManualActionProgress {
    enum Outcome { WAITING, SUCCEEDED, WARNING, INTERRUPTED }
    private final int commandSequence,pan,tilt;
    private final String kind;
    private final long baseline;
    ManualActionProgress(int commandSequence,String kind,int pan,int tilt,long baseline) {
        if(commandSequence<1 || commandSequence>65535) throw new IllegalArgumentException("Fahrbestätigung ohne Befehlsnummer");
        this.commandSequence=commandSequence; this.kind=kind; this.pan=pan; this.tilt=tilt; this.baseline=baseline;
    }
    Outcome accept(ManualActionStatus status,long version) {
        if(status==null || version<=baseline || status.commandSequence!=commandSequence || !status.kind.equals(kind) ||
            kind.equals("MOVE") && (status.pan!=pan || status.tilt!=tilt) || status.active()) return Outcome.WAITING;
        return status.succeeded()?Outcome.SUCCEEDED:status.result.equals("TARGET_MISSED")?Outcome.WARNING:Outcome.INTERRUPTED;
    }
}
