package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Explicit hardware diagnostic: HELLO/heartbeat only, no movement or camera commands. */
final class ConnectionChecks {
    private volatile boolean connected;
    private volatile String failure, firmware;
    private volatile long lastStatus;
    private volatile long lastManualStatus;
    private volatile boolean manualStatusSupported;
    private volatile int activeCycle;
    void run(Instrumentation instrumentation,long delayMs,boolean secure,String expectedFirmware) throws Exception {
        if (delayMs<0 || delayMs>120000) throw new IllegalArgumentException("Diagnose-Wartezeit");
        Context context = instrumentation.getTargetContext();
        String address = context.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("device",null);
        if (address == null) throw new AssertionError("Kein gespeichertes Gerät");
        if (delayMs>3000) pause(instrumentation,delayMs);
        for (int cycle=1; cycle<=2; cycle++) {
            final int cycleId=cycle; activeCycle=cycleId;
            connected=false; failure=null; firmware=null; lastStatus=lastManualStatus=0; manualStatusSupported=false;
            CountDownLatch ready = new CountDownLatch(1);
            DeviceState capabilities=new DeviceState();
            BluetoothSession session = new BluetoothSession(new BluetoothSession.Listener() {
                public void onAwaitingProtocol() { }
                public void onConnected() { if (activeCycle!=cycleId) return; connected=true; ready.countDown(); }
                public void onDisconnected(String reason) { if (activeCycle!=cycleId) return; connected=false; failure=reason; ready.countDown(); }
                public void onFrame(P2Codec.Frame frame) {
                    if (activeCycle!=cycleId) return;
                    capabilities.accept(frame,SystemClock.elapsedRealtime());
                    if (frame.type.equals("DEVICE_INFO")) firmware=frame.fields[0];
                    if (frame.type.equals("STATUS")) lastStatus=SystemClock.elapsedRealtime();
                    if(frame.type.equals("MANUAL_STATUS_CAPABILITIES")) manualStatusSupported=frame.fields.length==1 && frame.fields[0].equals("1");
                    if(frame.type.equals("MANUAL_STATUS")) { ManualActionStatus.parse(frame.fields); lastManualStatus=SystemClock.elapsedRealtime(); }
                }
            },secure);
            try {
                Bundle start = new Bundle(); start.putString("stream","\nVerbindungszyklus "+cycle+" startet ("+(secure?"secure":"insecure")+")\n"); instrumentation.sendStatus(0,start);
                session.connect(context.getSystemService(BluetoothManager.class).getAdapter().getRemoteDevice(address));
                if (!ready.await(20,TimeUnit.SECONDS) || !connected) throw new AssertionError("Zyklus "+cycle+": "+failure);
                long end = SystemClock.elapsedRealtime()+15000;
                while (SystemClock.elapsedRealtime()<end) {
                    Thread.sleep(500);
                    if (!connected) throw new AssertionError("Sitzung abgebrochen: "+failure);
                    if (lastStatus!=0 && SystemClock.elapsedRealtime()-lastStatus>2500) throw new AssertionError("Statusdaten veraltet");
                }
                if (lastStatus==0 || !expectedFirmware.equals(firmware)) throw new AssertionError("Erwartete Firmware "+expectedFirmware+", empfangen "+firmware);
                if(!manualStatusSupported || lastManualStatus==0 || SystemClock.elapsedRealtime()-lastManualStatus>2500)
                    throw new AssertionError("Aktueller manueller Betriebsstatus fehlt");
                if(expectedFirmware.equals("2.4.70") && (capabilities.runMaximumTempo!=2000 || !capabilities.warningSettingsSupported))
                    throw new AssertionError("Neue Lauf-/Warnkonfiguration auf dem echten Gerät nicht bestätigt");
                Bundle ok = new Bundle(); ok.putString("stream","\nZyklus "+cycle+": Firmware "+firmware+", 15 Sekunden aktuelle STATUS- und MANUAL_STATUS-Daten; Lauftempo bis "+capabilities.runMaximumTempo+" ‰, Warnschwellen "+capabilities.warningPanMdeg+"/"+capabilities.warningTiltMdeg+" m°, Config "+capabilities.motionConfigRevision+"\n"); instrumentation.sendStatus(0,ok);
            } finally { activeCycle=0; session.close(); }
            // Closing posts its final callback to the main thread; drain it before resetting this probe.
            instrumentation.waitForIdleSync();
            if (cycle==1) pause(instrumentation,delayMs);
        }
    }
    private void pause(Instrumentation instrumentation,long delayMs) throws InterruptedException {
        Bundle waiting=new Bundle(); waiting.putString("stream","\nWarte "+delayMs/1000+" Sekunden vor dem nächsten Funkaufbau\n");
        instrumentation.sendStatus(0,waiting); Thread.sleep(delayMs);
    }
}
