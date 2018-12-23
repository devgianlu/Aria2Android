package com.gianlu.aria2android;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.commonutils.Preferences.Prefs;

import java.io.File;

import androidx.annotation.NonNull;

public final class Aria2Compat {

    private Aria2Compat() {
    }

    public static void loadEnv(@NonNull Aria2Ui aria2) throws BadEnvironmentException {
        String envPath = Prefs.getString(PK.ENV_LOCATION, null);
        if (envPath == null) throw new MissingEnvException();
        aria2.loadEnv(new File(envPath));
    }

    public static class MissingEnvException extends BadEnvironmentException {
        MissingEnvException() {
        }
    }
}
