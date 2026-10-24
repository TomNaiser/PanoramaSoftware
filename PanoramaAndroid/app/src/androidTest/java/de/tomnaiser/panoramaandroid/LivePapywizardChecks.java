package de.tomnaiser.panoramaandroid;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilderFactory;

/** Real SQLite and external-media atomic replacement; never opens a transport or an Activity. */
final class LivePapywizardChecks {
    private int checks;
    private void check(boolean ok,String message) { if(!ok) throw new AssertionError(message);checks++; }
    private static LivePapywizard.Source reader(Context context,String name) {
        CaptureStore reader=new CaptureStore(context,name);
        return new LivePapywizard.Source() {
            public List<String> roots() { return reader.liveRoots(); }
            public LivePapywizard.Snapshot snapshot(String key) { return reader.liveSnapshot(key); }
            public void close() { reader.close(); }
        };
    }
    private static void feed(CaptureStore store,DeviceState state,String type,String... fields) throws Exception {
        if(type.equals("IMAGE_COMPLETED") && fields.length!=12) throw new AssertionError("Test fixture: IMAGE_COMPLETED needs 12 fields");
        P2Codec.Frame frame=P2Codec.decode(P2Codec.encode(1,type,fields));state.accept(frame,1000);store.accept("LIVE_TEST",state,frame);
    }
    private String content(File file,int count) throws Exception {
        org.w3c.dom.NodeList images=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("pict");
        check(images.getLength()==count,"Complete XML: expected "+count+" pictures, actual "+images.getLength());
        if(count>0) check(images.item(count-1).getAttributes().getNamedItem("id").getNodeValue().equals(""+(count-1)),"Last picture identity");
        return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
    }
    int run(android.app.Instrumentation runner,int iterations) throws Exception {
        if(iterations<1 || iterations>1000) throw new IllegalArgumentException("iterations: 1..1000");
        Context context=runner.getTargetContext();
        String name="live-"+UUID.randomUUID()+".db";
        File directory=new File(context.getExternalMediaDirs()[0],"Papywizard-checks-"+UUID.randomUUID());
        AtomicBoolean fail=new AtomicBoolean();long started=android.os.SystemClock.elapsedRealtime();
        PanoramaPlan plan=PanoramaPlan.parse("5|1|0|0|0|100|10|1000|1000|500|300|300|500|700|0|0".split("\\|"));
        try(CaptureStore store=new CaptureStore(context,name)) {
            DeviceState state=new DeviceState();
            try(LivePapywizard live=new LivePapywizard(reader(context,name),directory,(d,s) -> {
                if(fail.get()) throw new IOException("Injected full disk");LivePapywizard.atomicPublish(d,s);
            },status -> { })) {
                store.onExportChanged(live::request);
                feed(store,state,"PLAN",plan.fields());live.awaitIdle(10000);
                String root=store.liveRoots().get(0);
                File file=new File(directory,store.liveSnapshot(root).filename);
                content(file,0);
                for(int n=1;n<=iterations;n++) {
                    int[] target=plan.target(n-1);
                    String[] image={""+n,"5",""+n,"0",""+(n-1),""+target[2],""+target[3],""+target[0],""+target[1],""+(target[0]+13),""+(target[1]-7),"0"};
                    feed(store,state,"IMAGE_COMPLETED",image);live.awaitIdle(10000);
                    String xml=content(file,n);
                    check(xml.equals(store.exportLinked(root,true)),"Automatic and manual export match at "+n);
                    if(n==1) {
                        feed(store,state,"IMAGE_COMPLETED",image);live.awaitIdle(10000);content(file,1);
                    }
                    if(n%100==0) {
                        android.os.Bundle progress=new android.os.Bundle();progress.putString("stream","\nLive XML: "+n+"/"+iterations+" complete snapshots\n");runner.sendStatus(0,progress);
                    }
                }
                fail.set(true);
                feed(store,state,"MANUAL_IMAGE_TRIGGERED","1001","5","1","1000","2000","3000","1234","0");
                live.awaitIdle(10000);content(file,iterations);
                check(live.status().contains("fehlgeschlagen"),"Disk failure visible, acquisition data retained");
                check(store.exportLinked(root,true).contains("pitch=\"3.000\" yaw=\"-2.000\""),"Actual manual coordinates retained despite file failure");
                store.onExportChanged(key -> { });
            }
            try(LivePapywizard restarted=new LivePapywizard(reader(context,name),directory,status -> { })) {
                restarted.rebuild();restarted.awaitIdle(10000);
                String root=store.liveRoots().get(0);File file=new File(directory,store.liveSnapshot(root).filename);
                content(file,iterations+1);
                check(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8).equals(store.exportLinked(root,true)),"Restart repairs unpublished database commit");
                CaptureStore.Checkpoint checkpoint=new CaptureStore.Checkpoint(root,"LIVE_TEST",1,0,"FAULT_TILT_SENSOR",plan,iterations-1);
                PanoramaPlan rest=checkpoint.raster.remaining(6,false);
                String child=store.linkRest(checkpoint,rest,checkpoint.raster.startColumn(false));
                store.onExportChanged(restarted::request);
                feed(store,state,"IMAGE_COMPLETED","1002","6","1","0","0","0","0","0","0","456","789","0");
                restarted.awaitIdle(10000);
                check(!restarted.status().contains("fehlgeschlagen"),"Recovery writer status: "+restarted.status());
                check(store.liveSnapshot(child).xml.equals(store.exportLinked(root,true)),"Child and root snapshot content agree");
                content(file,iterations+2);
                check(store.liveSnapshot(child).filename.equals(file.getName()),"Recovery appends to same logical file");
                check(directory.list().length==1,"One finished XML for linked runs; no temp files");
                check(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8).equals(store.exportLinked(child,true)),"Linked export equality");
                String[] next=plan.fields();next[0]="7";
                feed(store,state,"PLAN",next);
                feed(store,state,"STATUS","7","RUNNING","MOVING","2","1000","0","2","0","0","0","0","0","2","NONE","2","0","1","2");
                restarted.awaitIdle(10000);
                String reconstructed=store.runs().stream().filter(r -> !r.key.equals(root) && !r.key.equals(child)).findFirst().get().key;
                File reconstructedFile=new File(directory,store.liveSnapshot(reconstructed).filename);
                content(reconstructedFile,2);
                check(store.exportLinked(reconstructed,false).contains("RECONSTRUCTED_RASTER_TARGET"),"Missing events reconstructed from confirmed count");
                feed(store,state,"IMAGE_COMPLETED","1003","7","1","0","0","0","0","0","0","321","654","0");
                restarted.awaitIdle(10000);
                String replaced=content(reconstructedFile,2);
                check(replaced.contains("pitch=\"0.654\" yaw=\"-0.321\""),"Late measured event updates same XML identity");
                check(replaced.equals(store.exportLinked(reconstructed,true)),"Reconstructed/measured automatic export matches manual export");
                store.onExportChanged(key -> { });
            }
        } finally {
            context.deleteDatabase(name);
            File[] files=directory.listFiles();if(files!=null) for(File file:files) if(!file.delete()) throw new IOException("Test cleanup failed: "+file);
            if(directory.exists() && !directory.delete()) throw new IOException("Test directory cleanup failed");
        }
        android.os.Bundle progress=new android.os.Bundle();progress.putString("stream","\n"+iterations+" real SQLite/XML updates + recovery/failure checks: "+(android.os.SystemClock.elapsedRealtime()-started)+" ms; no hardware commands.\n");runner.sendStatus(0,progress);
        return checks;
    }
}
