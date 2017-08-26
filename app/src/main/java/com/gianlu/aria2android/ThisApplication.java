package com.gianlu.aria2android;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;

public class ThisApplication extends Application {
    public static final String CATEGORY_USER_INPUT = "User input";
    public static final String CATEGORY_TIMING = "Timings";
    public static final String LABEL_SESSION_DURATION = "Session duration";
    public static final String ACTION_TURN_ON = "aria2 turned on";
    public static final String ACTION_TURN_OFF = "aria2 turned off";
    public static final String ACTION_OPENED_ARIA2APP = "Opened Aria2App";
    private static Tracker tracker;

    @NonNull
    private static Tracker getTracker(Application application) {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(application.getApplicationContext());
            analytics.enableAutoActivityReports(application);
            tracker = analytics.newTracker(R.xml.tracking);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableExceptionReporting(true);
        }

        return tracker;
    }

    public static void sendAnalytics(Context context, @Nullable Map<String, String> map) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("a2_trackingDisable", false) && !BuildConfig.DEBUG)
            if (tracker != null)
                tracker.send(map);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CommonUtils.setDebug(BuildConfig.DEBUG);
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG);
        tracker = getTracker(this);
    }
}
