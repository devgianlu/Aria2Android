package com.gianlu.aria2android;


import com.gianlu.aria2lib.Aria2PK;
import com.gianlu.commonutils.preferences.Prefs;

public final class PK extends Aria2PK {
    public static final Prefs.Key CURRENT_SESSION_START = new Prefs.Key("currentSessionStart");
    public static final Prefs.KeyWithDefault<Boolean> START_AT_BOOT = new Prefs.KeyWithDefault<>("startAtBoot", false);
    public static final Prefs.KeyWithDefault<Boolean> START_WITH_APP = new Prefs.KeyWithDefault<>("startWithApp", false);
    static final Prefs.Key IS_NEW_BUNDLED_WITH_ARIA2APP = new Prefs.Key("bundledWithAria2App");
}
