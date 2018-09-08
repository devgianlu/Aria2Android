package com.gianlu.aria2android;


import com.gianlu.commonutils.CommonPK;
import com.gianlu.commonutils.Preferences.Prefs;

public final class PK extends CommonPK {
    public static final Prefs.KeyWithDefault<Boolean> CUSTOM_BIN = new Prefs.KeyWithDefault<>("hasCustomBin", false);
    public static final Prefs.KeyWithDefault<Boolean> SHOW_PERFORMANCE = new Prefs.KeyWithDefault<>("showPerformance", true);
    public static final Prefs.KeyWithDefault<Integer> RPC_PORT = new Prefs.KeyWithDefault<>("rpcPort", 6800);
    public static final Prefs.KeyWithDefault<String> RPC_TOKEN = new Prefs.KeyWithDefault<>("rpcToken", "aria2");
    public static final Prefs.KeyWithDefault<Boolean> SAVE_SESSION = new Prefs.KeyWithDefault<>("saveSession", true);
    public static final Prefs.KeyWithDefault<Integer> NOTIFICATION_UPDATE_DELAY = new Prefs.KeyWithDefault<>("updateDelay", 1);
    public static final Prefs.KeyWithDefault<Boolean> START_AT_BOOT = new Prefs.KeyWithDefault<>("startAtBoot", false);
    public static final Prefs.KeyWithDefault<Boolean> RPC_ALLOW_ORIGIN_ALL = new Prefs.KeyWithDefault<>("allowOriginAll", false);
    public static final Prefs.Key CURRENT_SESSION_START = new Prefs.Key("currentSessionStart");
    public static final Prefs.Key CUSTOM_OPTIONS = new Prefs.Key("customOptions");
    public static final Prefs.Key OUTPUT_DIRECTORY = new Prefs.Key("outputPath");
    @Deprecated
    public static final Prefs.Key DEPRECATED_USE_CONFIG = new Prefs.Key("useConfig");
    @Deprecated
    public static final Prefs.Key DEPRECATED_CONFIG_FILE = new Prefs.Key("configFile");
}
