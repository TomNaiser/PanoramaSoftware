package de.tomnaiser.panoramaandroid;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit, hardware-free language checks against the real Activity and Service. */
final class LanguageUiChecks {
    private Instrumentation runner;
    private final AtomicReference<MainActivity> current=new AtomicReference<>();
    private int assertions;
    int run(Instrumentation instrumentation) throws Exception {
        runner=instrumentation;
        Context context=runner.getTargetContext();
        SharedPreferences preferences=context.getSharedPreferences("settings",Context.MODE_PRIVATE);
        String previous=preferences.getString(UiText.PREFERENCE,"de");
        Application app=(Application)context.getApplicationContext();
        Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity activity,Bundle state) { }
            public void onActivityResumed(Activity activity) { if(activity instanceof MainActivity) current.set((MainActivity)activity); }
            public void onActivityStarted(Activity activity) { }
            public void onActivityPaused(Activity activity) { }
            public void onActivityStopped(Activity activity) { }
            public void onActivitySaveInstanceState(Activity activity,Bundle state) { }
            public void onActivityDestroyed(Activity activity) { }
        };
        main(() -> app.registerActivityLifecycleCallbacks(lifecycle));
        try {
            preferences.edit().putString(UiText.PREFERENCE,"de").commit();
            double[] viewportBefore=new double[4];
            MainActivity german=launch(context);
            await(() -> read(german,"service")!=null);
            PanoramaService service=(PanoramaService)read(german,"service");
            main(() -> {
                check(!service.hasConnection(),"No real device connection");
                // Retain the same started service across Activity recreation, without a transport.
                context.startService(new Intent(context,PanoramaService.class));
                service.state.runId=77;
                service.state.state="PAUSED";
                inputs(german).get("profileName").setText("Gerät English camera");
                inputs(german).get("focal").setText("135.5");
                invoke(german,"selectPage",new Class<?>[]{int.class},4);
                PlanPreview preview=(PlanPreview)read(german,"preview");
                preview.viewport.fit(-30,30,-10,20);
                viewportBefore[0]=preview.viewport.left;viewportBefore[1]=preview.viewport.right;
                viewportBefore[2]=preview.viewport.bottom;viewportBefore[3]=preview.viewport.top;
                preview.selection.propose(12.345,-4.5,0,-180,180,-90,90);
                check(((TextView)german.findViewById(android.R.id.content).findViewWithTag("appLanguage")).getText().toString().contains("Deutsch"),"German language picker");
                check(service.connectionText().equals("Nicht verbunden"),"German retained status");
            });
            select(german,1);
            MainActivity english=recreated(german);
            main(() -> {
                check(read(english,"service")==service,"Service retained across language switch");
                check(service.state.runId==77 && service.state.state.equals("PAUSED"),"Device job retained");
                check(read(english,"selectedPage").equals(4),"Selected page retained");
                check(inputs(english).get("profileName").getText().toString().equals("Gerät English camera"),"User text retained verbatim");
                check(inputs(english).get("focal").getText().toString().equals("135.5"),"Numeric input retained");
                PlanPreview preview=(PlanPreview)read(english,"preview");
                check(preview.selection.pending && preview.selection.pan==12.345 && preview.selection.tilt==-4.5,"Pending target retained");
                check(preview.viewport.left==viewportBefore[0] && preview.viewport.right==viewportBefore[1] && preview.viewport.bottom==viewportBefore[2] && preview.viewport.top==viewportBefore[3],"Chart viewport retained");
                check(service.connectionText().equals("Not connected"),"Retained status changes language");
                check(((TextView)read(english,"connection")).getText().toString().equals("Not connected"),"Visible English status");
                check(english.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("en"),"English Android dialog context");
                @SuppressWarnings("unchecked") java.util.List<Button> tabs=(java.util.List<Button>)read(english,"tabs");
                check(tabs.get(0).getText().toString().equals("Device") && tabs.get(3).getText().toString().equals("Run"),"English navigation");
            });
            check(preferences.getString(UiText.PREFERENCE,"").equals("en"),"English preference persisted");
            main(english::finish);
            MainActivity restarted=launch(context);
            await(() -> read(restarted,"service")!=null);
            main(() -> check(((TextView)read(restarted,"connection")).getText().toString().equals("Not connected"),"English after reopening"));
            select(restarted,0);
            MainActivity restored=recreated(restarted);
            main(() -> check(((TextView)read(restored,"connection")).getText().toString().equals("Nicht verbunden"),"Switch back to German"));
            check(preferences.getString(UiText.PREFERENCE,"").equals("de"),"German preference persisted");
            return assertions;
        } finally {
            main(() -> {
                MainActivity activity=current.get();if(activity!=null) activity.finish();
                app.unregisterActivityLifecycleCallbacks(lifecycle);
                context.stopService(new Intent(context,PanoramaService.class));
                preferences.edit().putString(UiText.PREFERENCE,previous).commit();UiText.select(previous);
            });
        }
    }
    private void main(Runnable action) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        runner.runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { failure.set(ex); } });
        if(failure.get()!=null) throw new AssertionError(failure.get());
    }
    private MainActivity launch(Context context) {
        return (MainActivity)runner.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    private void select(MainActivity activity,int index) {
        main(() -> {
            AlertDialog dialog=(AlertDialog)invoke(activity,"chooseLanguage",new Class<?>[0]);
            ListView list=dialog.getListView();
            list.performItemClick(list.getChildAt(index),index,index);
        });
    }
    private MainActivity recreated(MainActivity old) throws Exception {
        await(() -> current.get()!=null && current.get()!=old && read(current.get(),"service")!=null);
        return current.get();
    }
    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+8000;
        AtomicReference<Boolean> ready=new AtomicReference<>(false);
        do {
            main(() -> ready.set(condition.getAsBoolean()));
            if(ready.get()) return;
            SystemClock.sleep(50);
        } while(SystemClock.elapsedRealtime()<deadline);
        throw new AssertionError("Timed out waiting for Activity/service");
    }
    @SuppressWarnings("unchecked") private Map<String,EditText> inputs(MainActivity activity) { return (Map<String,EditText>)read(activity,"inputs"); }
    private Object read(Object owner,String name) {
        try { Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner); }
        catch(Exception ex) { throw new AssertionError(ex); }
    }
    private Object invoke(Object owner,String name,Class<?>[] types,Object... values) {
        try { Method method=owner.getClass().getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(owner,values); }
        catch(Exception ex) { throw new AssertionError(ex); }
    }
    private void check(boolean value,String description) { if(!value) throw new AssertionError(description);assertions++; }
}
