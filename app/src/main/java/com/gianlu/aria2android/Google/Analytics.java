package com.gianlu.aria2android.Google;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.gianlu.aria2android.BuildConfig;
import com.gianlu.aria2android.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class Analytics {
    public static final String CATEGORY_USER_INPUT = "User input";
    public static final String ACTION_TURN_ON = "aria2 turned on";
    public static final String ACTION_TURN_OFF = "aria2 turned off";
    public static final String ACTION_OPENED_ARIA2APP = "Opened Aria2App";
    private static Tracker tracker = null;

    public static Tracker getDefaultTracker(Application application) {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(application.getApplicationContext());
            analytics.enableAutoActivityReports(application);
            tracker = analytics.newTracker(R.xml.tracking);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableExceptionReporting(true);
        }

        return tracker;
    }

    @Nullable
    static Tracker getTracker() {
        return tracker;
    }

    public static boolean isTrackingAllowed(Context context) {
        return !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_trackingDisable", false) && !BuildConfig.DEBUG;
    }
}
