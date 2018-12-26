package com.gianlu.aria2android;


import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import java.io.File;

public class ThisApplication extends AnalyticsApplication {

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Prefs.getBoolean(PK.IS_NEW_V2, true)) {
            File oldExec = new File(getFilesDir(), "aria2c");
            File newExec = new File(getFilesDir(), "env/aria2c");
            if (newExec.exists()) {
                oldExec.deleteOnExit();
            } else {
                if (oldExec.exists()) {
                    if (!oldExec.renameTo(newExec))
                        Logging.log("Failed migrating old executable!", true);
                }
            }

            Prefs.putBoolean(PK.IS_NEW_V2, false);
        }
    }
}
