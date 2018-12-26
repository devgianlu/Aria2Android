package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.commonutils.Preferences.Prefs;

public final class PK extends Aria2PK {
    public static final Prefs.KeyWithDefault<Boolean> CUSTOM_BIN = new Prefs.KeyWithDefault<>("hasCustomBin", false);
    public static final Prefs.Key CURRENT_SESSION_START = new Prefs.Key("currentSessionStart");
    public static final Prefs.KeyWithDefault<Boolean> START_AT_BOOT = new Prefs.KeyWithDefault<>("startAtBoot", false);
}
