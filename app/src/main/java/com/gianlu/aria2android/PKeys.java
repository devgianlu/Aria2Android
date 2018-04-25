package com.gianlu.aria2android;


import android.support.annotation.NonNull;

import com.gianlu.commonutils.Preferences.Prefs;

public enum PKeys implements Prefs.PrefKey {
    SHOW_PERFORMANCE("showPerformance"),
    CUSTOM_BIN("hasCustomBin"),
    OUTPUT_DIRECTORY("outputPath"),
    RPC_PORT("rpcPort"),
    RPC_TOKEN("rpcToken"),
    SAVE_SESSION("saveSession"),
    NOTIFICATION_UPDATE_DELAY("updateDelay"),
    START_AT_BOOT("startAtBoot"),
    CURRENT_SESSION_START("currentSessionStart"),
    CUSTOM_OPTIONS("customOptions"),
    DEPRECATED_USE_CONFIG("useConfig"),
    DEPRECATED_CONFIG_FILE("configFile"),
    RPC_ALLOW_ORIGIN_ALL("allowOriginAll");

    private final String key;

    PKeys(String key) {
        this.key = key;
    }

    @NonNull
    @Override
    public String getKey() {
        return key;
    }
}
