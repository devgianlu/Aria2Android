package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.Prefs;

import java.io.File;

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

            Prefs.putString(Aria2PK.ENV_LOCATION, new File(getFilesDir(), "env").getAbsolutePath());
            Prefs.putBoolean(PK.IS_NEW_V2, false);
        }
    }
}
