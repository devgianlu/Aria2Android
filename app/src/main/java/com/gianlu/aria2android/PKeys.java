package com.gianlu.aria2android;

import com.gianlu.commonutils.Prefs;

public enum PKeys implements Prefs.PrefKey {
    SHOW_PERFORMANCE("showPerformance"),
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

    @Override
    public String getKey() {
        return key;
    }
}
