package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.aria2lib.Aria2Ui;
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
