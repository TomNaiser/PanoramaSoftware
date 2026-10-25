package de.tomnaiser.panoramaandroid;

import android.app.Application;

/** Load the explicit app language before activities or the background service are created. */
public final class PanoramaApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        UiText.select(getSharedPreferences("settings",MODE_PRIVATE).getString(UiText.PREFERENCE,"de"));
    }
}
