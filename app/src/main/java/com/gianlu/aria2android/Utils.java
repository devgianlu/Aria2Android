package com.gianlu.aria2android;

import android.content.Context;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.NameValuePair;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Utils {
    public static final String LABEL_SESSION_DURATION = "session_duration";
    public static final String ACTION_TURN_ON = "aria2_on";
    public static final String ACTION_TURN_OFF = "aria2_off";
    public static final String ACTION_OPENED_ARIA2APP = "opened_aria2app";
    public static final String EVENT_UNKNOWN_LOG_LINE = "read_unknown_log_line";
    public static final String LABEL_LOG_LINE = "log_line";
    public static final String ACTION_IMPORT_BIN = "imported_bin";

    @NonNull
    public static Aria2Ui createAria2(@NonNull Context context, @Nullable Aria2Ui.Listener listener) {
        Aria2Ui ui = new Aria2Ui(context, listener);
        ui.setup(R.mipmap.ic_launcher, R.drawable.ic_notification, MainActivity.class);
        return ui;
    }

    public static String optionsBuilder(@Nullable Map<String, String> options) {
        if (options == null || options.isEmpty()) return "";
        StringBuilder extended = new StringBuilder();

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            extended.append(" --").append(entry.getKey()).append("=").append(entry.getValue());
        }

        return extended.toString();
    }

    public static List<NameValuePair> parseOptions(String str) {
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

    public static void toMap(JSONObject obj, Map<String, String> map) {
        Iterator<String> iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            map.put(key, obj.optString(key, null));
        }
    }
}