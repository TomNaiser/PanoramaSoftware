package de.tomnaiser.panoramaandroid;

import org.junit.After;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import static org.junit.Assert.*;

public final class UiTextTest {
    @After public void restoreGerman() { UiText.select("de"); }

    @Test public void explicitLanguageOverridesSystemLocaleAndUnknownSettingsFallBackToGerman() {
        Locale original=Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPAN);
            UiText.select("en");
            assertEquals("Device",UiText.DEVICE.text());
            assertEquals("1.25",String.format(UiText.locale(),"%.2f",1.25));
            UiText.select(null);
            assertEquals("Gerät",UiText.DEVICE.text());
            assertEquals("1,25",String.format(UiText.locale(),"%.2f",1.25));
            UiText.select("unsupported");
            assertEquals(UiText.Language.GERMAN,UiText.language());
        } finally { Locale.setDefault(original); }
    }

    @Test public void translationsPreserveFormattingArgumentsAndLineBreaks() {
        for(UiText text:UiText.values()) {
            String german=text.in(UiText.Language.GERMAN),english=text.in(UiText.Language.ENGLISH);
            assertFalse(text.name(),german.trim().isEmpty());
            assertFalse(text.name(),english.trim().isEmpty());
            assertEquals(text.name(),formats(german),formats(english));
            assertEquals(text.name(),german.chars().filter(c -> c=='\n').count(),english.chars().filter(c -> c=='\n').count());
        }
    }
    private List<String> formats(String text) {
        Matcher matcher=Pattern.compile("%[+.0-9]*[sdf]").matcher(text);
        List<String> result=new ArrayList<>();
        while(matcher.find()) result.add(matcher.group());
        return result;
    }

    @Test public void retainedStatusKeepsEventValuesAndChangesLanguage() {
        int[] ordinal={7};
        UiText.Message message=UiText.capture(() -> UiText.CAPTURE.text()+ordinal[0]);
        ordinal[0]=8;
        UiText.select("en");assertEquals("Capture 7",message.text());
        assertEquals("Not connected",UiText.existing("Nicht verbunden"));
        assertEquals("Gerät English camera",UiText.existing("Gerät English camera"));
        UiText.select("de");assertEquals("Aufnahme 7",message.text());
    }

    @Test public void bilingualCaptureDoesNotChangeBackgroundThreadsOrLeakAfterFailure() throws Exception {
        UiText.select("en");
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            UiText.capture(() -> {
                try { assertEquals("Device",worker.submit(UiText.DEVICE::text).get(2,TimeUnit.SECONDS)); }
                catch(Exception ex) { throw new AssertionError(ex); }
                return UiText.DEVICE.text();
            });
            try { UiText.capture(() -> { throw new IllegalArgumentException(); });fail(); }
            catch(IllegalArgumentException expected) { }
            assertEquals("Device",UiText.DEVICE.text());
        } finally { worker.shutdownNow(); }
    }

    @Test public void languageDoesNotChangeDeviceRecipesOrProtocolFrames() {
        UiText.select("de");
        PanoramaPlan german=plan();
        String frame=P2Codec.encode(17,"LOAD_PLAN",german.fields());
        UiText.select("en");
        PanoramaPlan english=plan();
        assertArrayEquals(german.fields(),english.fields());
        assertEquals(frame,P2Codec.encode(17,"LOAD_PLAN",english.fields()));
        assertArrayEquals(german.target(2),english.target(2));
        try { new CameraProfile("My camera",0,24,35.9,700,500);fail(); }
        catch(IllegalArgumentException expected) { assertEquals("Invalid camera profile",expected.getMessage()); }
    }
    private PanoramaPlan plan() { return PanoramaPlan.create(5,false,0,0,10,-10,5,5,0,700,30,500,500,false); }
}
