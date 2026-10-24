package de.tomnaiser.panoramaandroid;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.io.IOException;
import java.util.*;
import org.json.*;

/** Durable capture identities; XML and CSV are generated from the same ordered snapshot. */
public final class CaptureStore extends SQLiteOpenHelper {
    public static final class Run {
        public final String key, title;
        Run(String key, String title) { this.key = key; this.title = title; }
        @Override public String toString() { return title; }
    }
    public CaptureStore(Context context) { super(context, "captures.db", null, 4); }
    CaptureStore(Context context, String name) { super(context, name, null, 4); }
    private java.util.function.Consumer<String> exportChanged=key -> { };
    synchronized void onExportChanged(java.util.function.Consumer<String> listener) { exportChanged=listener; }
    /** Used only by the writer's separate database connection, so rendering never locks UI capture storage. */
    synchronized List<String> liveRoots() {
        List<String> roots=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM runs WHERE (plan IS NOT NULL OR EXISTS(SELECT 1 FROM captures WHERE run=runs.id)) AND NOT EXISTS(SELECT 1 FROM run_links WHERE child=runs.id) ORDER BY stamp",null)) {
            while(c.moveToNext()) roots.add(c.getString(0));
        }
        return roots;
    }
    synchronized LivePapywizard.Snapshot liveSnapshot(String selected) {
        String root=linkedKeys(selected).get(0);
        try(Cursor c=getReadableDatabase().rawQuery("SELECT run_id,stamp FROM runs WHERE id=?",new String[]{root})) {
            if(!c.moveToFirst()) throw new IllegalArgumentException("Gespeicherter Lauf fehlt");
            String filename="Panorama-"+c.getString(1).substring(0,10)+"-"+c.getString(0)+"-"+root+".xml";
            return new LivePapywizard.Snapshot(filename,exportLinked(root,true));
        }
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE runs (id TEXT PRIMARY KEY,device TEXT,run_id TEXT,stamp TEXT,plan TEXT,segments TEXT,completed INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE captures (run TEXT,capture_key TEXT,payload TEXT,ordinal INTEGER,manual INTEGER,logged TEXT,PRIMARY KEY(run,capture_key))");
        db.execSQL("CREATE INDEX capture_order ON captures(run,ordinal,manual)");
        createWarnings(db);
        createRecovery(db);
    }
    private void createWarnings(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE capture_warnings (run TEXT,ordinal INTEGER,flags INTEGER NOT NULL,PRIMARY KEY(run,ordinal))");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<1 || oldVersion>3 || newVersion!=4) throw new IllegalStateException("Unbekannte Datenbankversion");
        if(oldVersion<2) createWarnings(db);
        if(oldVersion<3) createRecovery(db);
        if(oldVersion==3) db.execSQL("ALTER TABLE run_faults ADD COLUMN tilt_zero INTEGER");
    }
    private void createRecovery(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS run_links (child TEXT PRIMARY KEY,parent TEXT NOT NULL,start_column INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS run_faults (run TEXT,boot INTEGER,tilt_zero INTEGER,image_index INTEGER,revision INTEGER,completed INTEGER,error TEXT,plan TEXT,status TEXT,logged TEXT,PRIMARY KEY(run,image_index,revision,completed,error))");
    }
    static final class Checkpoint {
        final String key,device,error;
        final long boot;
        final int tiltZero;
        final RasterRecovery raster;
        Checkpoint(String key,String device,long boot,int tiltZero,String error,PanoramaPlan plan,long index) {
            this.key=key;this.device=device;this.boot=boot;this.tiltZero=tiltZero;this.error=error;raster=new RasterRecovery(plan,index);
        }
    }
    /** Store only a complete device recipe; repeated FAULT status cannot duplicate an uncertain exposure. */
    synchronized boolean recordFault(String device,long boot,DeviceState state,String[] fields) {
        if(!(state.state.equals("FAULT") || RunRecoveryPause.active(state) ||
            state.state.equals("STOPPED") && RasterRecovery.recoverable(state.lastError)) || state.runId==0 || !state.tiltCalibrationKnown) return false;
        String key=group(device,state.runId);PanoramaPlan plan=state.plan;
        if(plan==null) try(Cursor c=getReadableDatabase().rawQuery("SELECT plan FROM runs WHERE id=?",new String[]{key})) {
            if(c.moveToFirst() && !c.isNull(0)) plan=PanoramaPlan.parse(c.getString(0).split("\\|",-1));
        }
        if(plan==null || plan.runId!=state.runId) return false;
        if(state.nextImageIndex>=plan.total()) return false;
        ContentValues v=new ContentValues();v.put("run",key);v.put("boot",boot);v.put("image_index",state.nextImageIndex);
        v.put("tilt_zero",state.tiltZeroOffset);
        v.put("revision",state.revision);v.put("completed",state.completed);v.put("error",state.lastError);
        v.put("plan",String.join("|",plan.fields()));v.put("status",String.join("|",fields));v.put("logged",java.time.Instant.now().toString());
        return getWritableDatabase().insertWithOnConflict("run_faults",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    synchronized Checkpoint checkpoint(String device,long run) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT f.run,f.boot,f.error,f.plan,f.image_index,f.tilt_zero FROM run_faults f JOIN runs r ON r.id=f.run WHERE r.device=? AND r.run_id=? ORDER BY f.logged DESC LIMIT 1",new String[]{device,""+run})) {
            if(!c.moveToFirst() || c.isNull(5) || !RasterRecovery.recoverable(c.getString(2))) return null;
            try { return new Checkpoint(c.getString(0),device,c.getLong(1),c.getInt(5),c.getString(2),PanoramaPlan.parse(c.getString(3).split("\\|",-1)),c.getLong(4)); }
            catch(IllegalArgumentException ex) { return null; }
        }
    }
    synchronized Checkpoint pendingCheckpoint(String device) {
        // Only the latest reserved, empty child can recover a CLEAR/LOAD interrupted by a lost link or process death.
        try(Cursor c=getReadableDatabase().rawQuery("SELECT p.run_id FROM runs r JOIN run_links l ON l.child=r.id JOIN runs p ON p.id=l.parent WHERE r.id=(SELECT id FROM runs WHERE device=? AND run_id!='0' ORDER BY stamp DESC LIMIT 1) AND r.completed=0 AND NOT EXISTS(SELECT 1 FROM captures WHERE run=r.id)",new String[]{device})) {
            return c.moveToFirst()?checkpoint(device,Long.parseLong(c.getString(0))):null;
        }
    }
    /** Reserve the child identity and its ancestry before sending LOAD_PLAN (also survives a process death). */
    synchronized String linkRest(Checkpoint source,PanoramaPlan rest,int startColumn) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            if((startColumn!=source.raster.startColumn(false) && startColumn!=source.raster.startColumn(true)) ||
                !source.raster.remaining(rest.runId,startColumn<source.raster.startColumn(false)).sameRecipe(rest))
                throw new IllegalArgumentException("Restauftrag passt nicht zum gespeicherten Raster");
            try(Cursor c=db.rawQuery("SELECT id FROM runs WHERE device=? AND run_id=?",new String[]{source.device,""+rest.runId})) {
                if(c.moveToFirst()) throw new IllegalArgumentException("Laufnummer bereits gespeichert");
            }
            String child=newGroup(source.device,rest.runId);
            ContentValues recipe=new ContentValues();recipe.put("plan",String.join("|",rest.fields()));updateRun(child,recipe);
            ContentValues link=new ContentValues();link.put("child",child);link.put("parent",source.key);link.put("start_column",startColumn);
            db.insertOrThrow("run_links",null,link);db.setTransactionSuccessful();return child;
        } finally { db.endTransaction(); }
    }
    private List<String> linkedKeys(String selected) {
        String root=selected;Set<String> seen=new HashSet<>();
        while(seen.add(root)) {
            try(Cursor c=getReadableDatabase().rawQuery("SELECT parent FROM run_links WHERE child=?",new String[]{root})) {
                if(!c.moveToFirst()) break;root=c.getString(0);
            }
        }
        List<String> result=new ArrayList<>();collectChildren(root,result,new HashSet<>());return result;
    }
    private void collectChildren(String key,List<String> result,Set<String> seen) {
        if(!seen.add(key)) throw new IllegalStateException("Zyklische Laufverknüpfung");
        result.add(key);
        List<String> children=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT l.child FROM run_links l JOIN runs r ON r.id=l.child WHERE l.parent=? ORDER BY r.stamp,r.id",new String[]{key})) {
            while(c.moveToNext()) children.add(c.getString(0));
        }
        for(String child:children) collectChildren(child,result,seen);
    }
    synchronized boolean hasUncertainExposure(String selected) {
        for(String key:linkedKeys(selected)) try(Cursor c=getReadableDatabase().rawQuery("SELECT error FROM run_faults WHERE run=?",new String[]{key})) {
            while(c.moveToNext()) if(RasterRecovery.uncertainExposure(c.getString(0))) return true;
        }
        return false;
    }
    synchronized String exportInterruptions(String selected) {
        StringBuilder out=new StringBuilder("run_key;run_id;parent_run_key;source_start_column;logged_utc;error;image_index;column;row;counted_captures;possible_extra_camera_file\n");
        for(String key:linkedKeys(selected)) try(Cursor c=getReadableDatabase().rawQuery("SELECT r.run_id,COALESCE(l.parent,''),COALESCE(l.start_column,0),f.logged,f.error,f.image_index,f.completed,f.plan FROM run_faults f JOIN runs r ON r.id=f.run LEFT JOIN run_links l ON l.child=r.id WHERE f.run=? ORDER BY f.logged",new String[]{key})) {
            while(c.moveToNext()) {
                PanoramaPlan p=PanoramaPlan.parse(c.getString(7).split("\\|",-1));long index=c.getLong(5);
                out.append(String.join(";",key,c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),""+index,
                    index<p.total()?""+p.target((int)index)[2]:"",index<p.total()?""+p.target((int)index)[3]:"",c.getString(6),RasterRecovery.uncertainExposure(c.getString(4))?"true":"false")).append('\n');
            }
        }
        return out.toString();
    }
    private String group(String device, long runId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id FROM runs WHERE device=? AND run_id=? ORDER BY stamp DESC LIMIT 1",
                new String[]{device, ""+runId})) { if (c.moveToFirst()) return c.getString(0); }
        return newGroup(device, runId);
    }
    private String newGroup(String device, long runId) {
        String id = UUID.randomUUID().toString();
        ContentValues values = new ContentValues(); values.put("id", id); values.put("device", device);
        values.put("run_id", ""+runId); values.put("stamp", java.time.Instant.now().toString());
        values.put("segments", "[]"); getWritableDatabase().insertOrThrow("runs", null, values); return id;
    }
    public synchronized boolean accept(String device, DeviceState state, P2Codec.Frame frame) throws IOException {
        String[] f = frame.fields;
        if (!(frame.type.equals("PLAN") || frame.type.equals("RUN_SEGMENT") || frame.type.equals("STATUS")
                || frame.type.equals("IMAGE_COMPLETED") || frame.type.equals("MANUAL_IMAGE_TRIGGERED") || frame.type.equals("CAPTURE_WARNING"))) return false;
        // Empty recipes from older stopped firmware contain no capture history.
        if(frame.type.equals("PLAN") && f.length==16 && f[0].equals("0")) return false;
        try {
            if(frame.type.equals("CAPTURE_WARNING")) {
                CaptureWarning warning=CaptureWarning.parse(f);
                ContentValues value=new ContentValues();value.put("run",group(device,warning.runId));
                value.put("ordinal",warning.ordinal);value.put("flags",warning.flags);
                return getWritableDatabase().insertWithOnConflict("capture_warnings",null,value,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
            }
            long runId = DeviceState.unsigned(frame.type.equals("STATUS") || frame.type.equals("PLAN") || frame.type.equals("RUN_SEGMENT") ? f[0] : f[1]);
            String key = group(device, runId);
            boolean changed=false;
            if (frame.type.equals("PLAN")) {
                PanoramaPlan.parse(f);
                ContentValues v = new ContentValues(); v.put("plan", String.join("|", f)); updateRun(key, v);
                changed=true;
            } else if (frame.type.equals("RUN_SEGMENT")) {
                if (f.length != 4) return false;
                JSONArray segments = segments(key); int revision = Integer.parseInt(f[1]);
                JSONObject segment = new JSONObject().put("revision", revision).put("start", DeviceState.unsigned(f[2])).put("count", DeviceState.unsigned(f[3]));
                boolean found = false;
                for (int i = 0; i < segments.length(); i++) if (segments.getJSONObject(i).getInt("revision") == revision) { segments.put(i, segment); found = true; break; }
                if (!found) segments.put(segment);
                ContentValues v = new ContentValues(); v.put("segments", segments.toString()); updateRun(key, v);
            } else if (frame.type.equals("STATUS")) {
                lastStatusCount = f[17];
                long previous = completed(key);
                if (state.completed < previous) {
                    key = newGroup(device, runId);
                    if (state.plan!=null) { ContentValues recipe = new ContentValues(); recipe.put("plan",String.join("|",state.plan.fields())); updateRun(key,recipe); }
                }
                ContentValues v = new ContentValues(); v.put("completed", state.completed); updateRun(key, v);
                if (state.total > 0 && state.completed > 0 && state.completed != previous) { reconstruct(key, state);changed=true; }
            } else {
                boolean manual = frame.type.equals("MANUAL_IMAGE_TRIGGERED");
                if ((!manual && f.length != 12) || (manual && f.length != 8)) return false;
                DeviceState.unsigned(f[2]); DeviceState.unsigned(f[0]);
                int positionIndex = manual ? 4 : 9;
                Integer.parseInt(f[positionIndex]); Integer.parseInt(f[positionIndex + 1]);
                String identity = manual ? "M-"+runId+"-"+f[0]+"-"+f[6] : "R-"+runId+"-"+f[2];
                changed=insert(key, identity, frame.type + "|" + String.join("|", f), DeviceState.unsigned(manual ? f[3] : f[2]), manual, true);
            }
            if(changed) exportChanged.accept(key);
        } catch (RuntimeException | JSONException ex) { throw new IOException("Aufnahmedaten: " + ex.getMessage(), ex); }
        return false;
    }
    private void updateRun(String key, ContentValues values) { getWritableDatabase().update("runs", values, "id=?", new String[]{key}); }
    private long completed(String key) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT completed FROM runs WHERE id=?", new String[]{key})) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }
    private JSONArray segments(String key) throws JSONException {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT segments FROM runs WHERE id=?", new String[]{key})) { return c.moveToFirst() ? new JSONArray(c.getString(0)) : new JSONArray(); }
    }
    private void reconstruct(String key, DeviceState state) throws JSONException {
        PanoramaPlan plan = null;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT plan FROM runs WHERE id=?", new String[]{key})) {
            if (c.moveToFirst() && !c.isNull(0)) plan = PanoramaPlan.parse(c.getString(0).split("\\|", -1));
        }
        if (plan == null) return;
        JSONArray segments = segments(key);
        List<long[]> ordered = new ArrayList<>();
        for (int i = 0; i < segments.length(); i++) {
            JSONObject segment = segments.getJSONObject(i);
            int rev = segment.getInt("revision");
            long count = rev == state.revision ? DeviceState.unsigned(lastStatusCount) : segment.getLong("count");
            ordered.add(new long[]{rev, segment.getLong("start"), count});
        }
        ordered.sort(Comparator.comparingLong(a -> a[0]));
        // Absence of segments is only reconstructible for the original, unrevised recipe.
        if (ordered.isEmpty() && state.revision == 0) ordered.add(new long[]{0, 0, state.completed});
        long capture = 0; int generated = 0;
        Set<String> existing = new HashSet<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT capture_key FROM captures WHERE run=? AND manual=0", new String[]{key})) {
            while (c.moveToNext()) existing.add(c.getString(0));
        }
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            for (long[] segment : ordered) for (long offset = 0; offset < segment[2] && capture < state.completed; offset++) {
                capture++; long index = segment[1] + offset;
                if (index >= plan.total() || ++generated > 100000) break;
                if (existing.contains("R-"+plan.runId+"-"+capture)) continue;
                int[] t = plan.target((int)index);
                String[] f = {"0", ""+plan.runId, ""+capture, ""+segment[0], ""+index, ""+t[2], ""+t[3],
                    ""+t[0], ""+t[1], ""+t[0], ""+t[1], "1"};
                insert(key, "R-"+plan.runId+"-"+capture, "IMAGE_COMPLETED|"+String.join("|", f), capture, false, false);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    private String lastStatusCount = "0";
    private boolean insert(String run, String identity, String payload, long ordinal, boolean manual, boolean measured) {
        ContentValues v = new ContentValues(); v.put("run", run); v.put("capture_key", identity);
        v.put("payload", payload); v.put("ordinal", ordinal); v.put("manual", manual ? 1 : 0);
        v.put("logged", java.time.Instant.now().toString());
        if (measured) {
            try (Cursor c = getReadableDatabase().rawQuery("SELECT payload FROM captures WHERE run=? AND capture_key=?", new String[]{run, identity})) {
                if (c.moveToFirst() && !c.getString(0).endsWith("|1")) return false;
            }
        }
        return getWritableDatabase().insertWithOnConflict("captures", null, v, measured ? SQLiteDatabase.CONFLICT_REPLACE : SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public synchronized List<Run> runs() {
        List<Run> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,run_id,stamp,device,completed,(SELECT count(*) FROM captures WHERE run=runs.id AND manual=0) FROM runs WHERE id IN (SELECT run FROM captures) ORDER BY stamp DESC", null)) {
            while (c.moveToNext()) result.add(new Run(c.getString(0), "Lauf " + c.getString(1) + " · " + c.getString(2) + " · " + c.getString(3) + " · Raster " + c.getLong(5) + "/" + c.getLong(4)));
        }
        return result;
    }
    synchronized List<RunPlotModel.Capture> plotCaptures(String device,long runId) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM runs WHERE device=? AND run_id=? ORDER BY stamp DESC LIMIT 1",new String[]{device,""+runId})) {
            return c.moveToFirst()?plotCaptures(c.getString(0)):Collections.emptyList();
        }
    }
    private List<RunPlotModel.Capture> plotCaptures(String key) {
        List<RunPlotModel.Capture> result=new ArrayList<>();
        String query="SELECT c.capture_key,c.payload,COALESCE(w.flags,0) FROM captures c LEFT JOIN capture_warnings w ON w.run=c.run AND w.ordinal=c.ordinal AND c.manual=0 WHERE c.run=? AND (w.flags>0 OR c.capture_key IN (SELECT capture_key FROM captures WHERE run=? ORDER BY ordinal DESC,manual DESC,logged DESC LIMIT 512)) ORDER BY c.ordinal,c.manual,c.logged";
        try(Cursor c=getReadableDatabase().rawQuery(query,new String[]{key,key})) {
            while(c.moveToNext()) result.add(RunPlotModel.Capture.parse(c.getString(0),c.getString(1),c.getInt(2)));
        }
        return result;
    }
    static final class PlotSnapshot {
        final PanoramaPlan plan; final RunPlotModel model; final String device;
        PlotSnapshot(PanoramaPlan plan,RunPlotModel model,String device) { this.plan=plan; this.model=model;this.device=device; }
    }
    synchronized PlotSnapshot plotSnapshot(String key) {
        PanoramaPlan plan=null; RunPlotModel model=new RunPlotModel();String device;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT run_id,plan,device FROM runs WHERE id=?",new String[]{key})) {
            if(!c.moveToFirst()) throw new IllegalArgumentException("Gespeicherter Lauf fehlt");
            model.runId=Long.parseLong(c.getString(0));
            device=c.getString(2);
            if(!c.isNull(1)) plan=PanoramaPlan.parse(c.getString(1).split("\\|",-1));
        }
        model.captures=plotCaptures(key); return new PlotSnapshot(plan,model,device);
    }
    public synchronized String export(String key, boolean xml) {
        return exportKeys(Collections.singletonList(key),xml,false);
    }
    public synchronized String exportLinked(String key,boolean xml) {
        return exportKeys(linkedKeys(key),xml,true);
    }
    private String exportKeys(List<String> keys,boolean xml,boolean linked) {
        StringBuilder out = new StringBuilder(xml ? "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<papywizard version=\"c\"><header/><shoot>\n"
            : "picture_id;camera_file_order;capture_key;run_id;event_sequence;capture_sequence;raster_capture_count;plan_revision;image_index;column;row;capture_type;position_source;device_time_ms;logged_utc;target_yaw_deg;target_pitch_deg;actual_yaw_deg;actual_pitch_deg;warning\n");
        if(linked && !xml) { out.setLength(out.length()-1);out.append(";source_run_key;parent_run_key;source_start_column;camera_file_order_uncertain;repeated_target\n"); }
        int id = 0;Set<String> seenTargets=new HashSet<>();boolean uncertain=linked && hasUncertainExposure(keys.get(0));
        for(String key:keys) {
        String parent="",startColumn="0";
        try(Cursor relation=getReadableDatabase().rawQuery("SELECT parent,start_column FROM run_links WHERE child=?",new String[]{key})) {
            if(relation.moveToFirst()) { parent=relation.getString(0);startColumn=relation.getString(1); }
        }
        try (Cursor c = getReadableDatabase().rawQuery("SELECT c.capture_key,c.payload,c.logged,COALESCE(w.flags,0) FROM captures c LEFT JOIN capture_warnings w ON w.run=c.run AND w.ordinal=c.ordinal AND c.manual=0 WHERE c.run=? ORDER BY c.ordinal,c.manual,c.logged,c.capture_key", new String[]{key})) {
            while (c.moveToNext()) {
                String[] p = c.getString(1).split("\\|", -1); String[] f = Arrays.copyOfRange(p, 1, p.length);
                boolean manual = p[0].equals("MANUAL_IMAGE_TRIGGERED"), reconstructed = f[manual ? 7 : 11].equals("1");
                String yaw = degrees(f[manual ? 4 : 9]), pitch = tiltDegrees(f[manual ? 5 : 10]);
                if (xml) out.append("<pict id=\"").append(id).append("\" bracket=\"1\"><time>").append(c.getString(2))
                    .append("</time><position pitch=\"").append(pitch).append("\" yaw=\"").append(yaw)
                    .append("\" roll=\"0\"/></pict>\n");
                else {
                    String[] fields = {""+id, ""+(id+1), c.getString(0), f[1], f[0], f[2], manual ? f[3] : f[2],
                        manual ? "" : f[3], manual ? "" : f[4], manual ? "" : f[5], manual ? "" : f[6], manual ? "MANUAL" : "RASTER",
                        reconstructed ? (manual ? "STORED_MANUAL_POSITION" : "RECONSTRUCTED_RASTER_TARGET") : "MEASURED_LIVE",
                        manual ? f[6] : "", c.getString(2), manual ? "" : degrees(f[7]), manual ? "" : tiltDegrees(f[8]),
                        reconstructed && !manual ? "" : yaw, reconstructed && !manual ? "" : pitch,CaptureWarning.describe(c.getInt(3))};
                    out.append(String.join(";", fields));
                    if(linked) out.append(';').append(key).append(';').append(parent).append(';').append(startColumn).append(';').append(uncertain)
                        .append(';').append(!manual && !seenTargets.add(f[7]+"|"+f[8]));
                    out.append('\n');
                }
                id++;
            }
        }
        }
        if (xml) out.append("</shoot></papywizard>\n");
        return out.toString();
    }
    private static String degrees(String text) { return String.format(Locale.ROOT, "%.3f", -Integer.parseInt(text) / 1000.0); }
    private static String tiltDegrees(String text) { return String.format(Locale.ROOT,"%.3f",Integer.parseInt(text)/1000.0); }
}
