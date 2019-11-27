package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.logging.Logging;

public class ThisApplication extends AnalyticsApplication {

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Logging.clearLogs(this, 3);

        Aria2Ui.provider(Aria2BareConfig.class);
    }
}
