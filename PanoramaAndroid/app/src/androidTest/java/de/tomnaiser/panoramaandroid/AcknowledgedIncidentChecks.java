package de.tomnaiser.panoramaandroid;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Production service with fake wire, isolated package; never connects real hardware. */
final class AcknowledgedIncidentChecks {
    private PanoramaService service;
    private final List<String> failures=new ArrayList<>();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private int checks;
    int run(Instrumentation runner) throws Exception {
        Context context=runner.getTargetContext();
        if(!context.getPackageName().endsWith(".validation")) throw new AssertionError("Isolated validation package required");
        ServiceConnection binding=new ServiceConnection() {
            public void onServiceConnected(ComponentName n,IBinder b){service=((PanoramaService.LocalBinder)b).service();}
            public void onServiceDisconnected(ComponentName n){service=null;}
        };
        main(() -> {if(!context.bindService(new Intent(context,PanoramaService.class),binding,Context.BIND_AUTO_CREATE)) throw new AssertionError("Bind");});
        try {
            for(int i=0;service==null && i<50;i++) Thread.sleep(100);
            if(service==null) throw new AssertionError("Service missing");
            FakeLink fake=new FakeLink();
            main(() -> {
                if(service.hasConnection()) throw new AssertionError("Existing connection must remain untouched");
                service.incident.resolved();raise("CONNECTION","old connection event");
                long old=service.incident.id;service.incident.acknowledge(old);
                raise("QUALITY_DEVIATION","new quality event");
                check(service.incident.id>old && service.incident.needsAcknowledgement(),"New cause after acknowledged old incident must request its own acknowledgement");
                long next=service.incident.id;raise("QUALITY_DEVIATION","repeated quality status");
                check(service.incident.id==next,"Repeated same cause must retain one incident identity");
                service.incident.resolved();raise("CONNECTION","old connection event");service.incident.acknowledge(service.incident.id);
                service.connected=true;service.state=new DeviceState();service.state.compatible=true;
                service.state.state="RUNNING";service.state.runId=717;service.state.lastStatusMs=SystemClock.elapsedRealtime();
                set("session",fake);
                set("pendingQuality",new CaptureQualityGuard.Issue(new String[]{"1","717","1","0","0","0","0","0","0","0","2000","0"}));
                invoke("pauseForQuality",new Class<?>[0]);
                check(fake.commands.equals(Collections.singletonList("PAUSE")),"Acknowledged old incident must not block a new quality pause");
            });
            Thread.sleep(150);
            main(() -> {
                check(service.incident.key.endsWith("|QUALITY_DEVIATION") && service.incident.needsAcknowledgement(),"Confirmed quality pause must create the new unacknowledged incident");
                fake.commands.clear();service.incident.acknowledge(service.incident.id);service.uncertain=true;
                set("pendingQuality",new CaptureQualityGuard.Issue(new String[]{"2","717","2","0","1","0","1","0","0","0","2000","0"}));
                invoke("pauseForQuality",new Class<?>[0]);
                check(fake.commands.isEmpty(),"Uncertain command effect still blocks a new automatic action");
                service.uncertain=false;service.incident.observe("|SENSOR_FAULT","unacknowledged sensor incident");
                invoke("pauseForQuality",new Class<?>[0]);
                check(fake.commands.isEmpty(),"Unacknowledged device incident still has priority over quality pause");
            });
        } finally {
            main(() -> {if(service!=null) service.disconnect();context.unbindService(binding);});
        }
        if(!failures.isEmpty()) throw new AssertionError(String.join("; ",failures));
        return checks;
    }
    private void raise(String code,String detail) {invoke("raiseIncident",new Class<?>[]{String.class,String.class,boolean.class},code,detail,false);}
    private void invoke(String name,Class<?>[] types,Object... args) {
        try{Method m=PanoramaService.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(service,args);}
        catch(Exception ex){throw new AssertionError(ex);}
    }
    private void set(String name,Object value) {
        try{Field f=PanoramaService.class.getDeclaredField(name);f.setAccessible(true);f.set(service,value);}
        catch(Exception ex){throw new AssertionError(ex);}
    }
    private void main(Runnable action) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        ui.post(() -> {try{action.run();}catch(Throwable ex){error.set(ex);}finally{done.countDown();}});
        if(!done.await(5,TimeUnit.SECONDS)) throw new AssertionError("UI timeout");
        if(error.get()!=null) throw new AssertionError(error.get());
    }
    private void check(boolean value,String message){checks++;if(!value)failures.add(message);}
    private static final class FakeLink implements DeviceLink {
        final List<String> commands=new ArrayList<>();
        public CompletableFuture<V2Client.Result> send(String type,String... fields){
            if(!type.equals("PAUSE")) throw new AssertionError("Forbidden fake command: "+type);
            commands.add(type);return CompletableFuture.completedFuture(new V2Client.Result(true,"PAUSE_PENDING"));
        }
        public CompletableFuture<V2Client.Result> emergencyStop(String run){throw new AssertionError("Unexpected emergency action");}
        public void fail(String reason){throw new AssertionError(reason);}
        public void close(){}
    }
}
