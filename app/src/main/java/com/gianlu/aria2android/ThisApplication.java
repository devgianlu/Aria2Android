package com.gianlu.aria2android;


import androidx.annotation.Nullable;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.analytics.AnalyticsApplication;

public class ThisApplication extends AnalyticsApplication {

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Nullable
    @Override
    protected String getGithubProjectName() {
        return "Aria2Android";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Aria2Ui.provider(Aria2BareConfig.class);
    }
}
