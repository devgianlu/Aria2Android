package com.gianlu.aria2lib;

import android.os.Environment;

import com.gianlu.commonutils.CommonPK;
import com.gianlu.commonutils.Preferences.Prefs;

public class Aria2PK extends CommonPK {
    public static final Prefs.KeyWithDefault<Integer> NOTIFICATION_UPDATE_DELAY = new Prefs.KeyWithDefault<>("updateDelay", 1);
    public static final Prefs.KeyWithDefault<Boolean> SHOW_PERFORMANCE = new Prefs.KeyWithDefault<>("showPerformance", true);
    public static final Prefs.Key ENV_LOCATION = new Prefs.Key("envLocation");
    public static final Prefs.KeyWithDefault<Integer> RPC_PORT = new Prefs.KeyWithDefault<>("rpcPort", 6800);
    public static final Prefs.KeyWithDefault<String> RPC_TOKEN = new Prefs.KeyWithDefault<>("rpcToken", "aria2");
    public static final Prefs.KeyWithDefault<Boolean> RPC_ALLOW_ORIGIN_ALL = new Prefs.KeyWithDefault<>("allowOriginAll", false);
    public static final Prefs.KeyWithDefault<String> OUTPUT_DIRECTORY = new Prefs.KeyWithDefault<>("outputPath", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    public static final Prefs.Key CUSTOM_OPTIONS = new Prefs.Key("customOptions");
    public static final Prefs.KeyWithDefault<Boolean> SAVE_SESSION = new Prefs.KeyWithDefault<>("saveSession", true);
}
