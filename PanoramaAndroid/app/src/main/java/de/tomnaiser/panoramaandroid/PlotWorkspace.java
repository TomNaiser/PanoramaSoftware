package de.tomnaiser.panoramaandroid;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** The plot stays outside settings scrolling; expansion changes layout only. */
final class PlotWorkspace extends LinearLayout {
    private final LinearLayout diagram;
    private final ScrollView settings;
    private final boolean landscape;
    private boolean expanded;
    PlotWorkspace(Context context,LinearLayout diagram,ScrollView settings) {
        super(context); this.diagram=diagram; this.settings=settings;
        landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        setOrientation(landscape?HORIZONTAL:VERTICAL);
        addView(diagram); addView(settings); setExpanded(false);
    }
    boolean expanded() { return expanded; }
    void setExpanded(boolean value) {
        expanded=value; settings.setVisibility(value?View.GONE:View.VISIBLE);
        diagram.setLayoutParams(new LayoutParams(landscape?0:LayoutParams.MATCH_PARENT,landscape?LayoutParams.MATCH_PARENT:0,value?1:3));
        settings.setLayoutParams(new LayoutParams(landscape?0:LayoutParams.MATCH_PARENT,landscape?LayoutParams.MATCH_PARENT:0,2));
        requestLayout();
    }
}
