package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.analytics.AnalyticsApplication;

public class ThisApplication extends AnalyticsApplication {

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Aria2Ui.provider(Aria2BareConfig.class);
    }
}
