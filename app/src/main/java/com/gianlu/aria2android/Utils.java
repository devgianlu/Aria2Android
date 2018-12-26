package com.gianlu.aria2android;

import android.content.Context;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.NameValuePair;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Utils {
    public static final String LABEL_SESSION_DURATION = "session_duration";
    public static final String ACTION_TURN_ON = "aria2_on";
    public static final String ACTION_TURN_OFF = "aria2_off";
    public static final String ACTION_OPENED_ARIA2APP = "opened_aria2app";
    public static final String ACTION_IMPORT_BIN = "imported_bin";

    @NonNull
    public static Aria2Ui createAria2(@NonNull Context context, @Nullable Aria2Ui.Listener listener) {
        Aria2Ui ui = new Aria2Ui(context, listener);
        ui.setup(R.mipmap.ic_launcher, R.drawable.ic_notification, MainActivity.class);
        return ui;
    }

    public static List<NameValuePair> parseOptions(@NonNull String str) {
        List<NameValuePair> list = new ArrayList<>();
        String[] lines = str.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#")) continue;
            String[] split = line.split("=");
            if (split.length > 0)
                list.add(new NameValuePair(split[0], split.length == 1 ? null : split[1]));
        }

        return list;
    }
}