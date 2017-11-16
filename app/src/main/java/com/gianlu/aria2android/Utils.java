package com.gianlu.aria2android;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Toaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Utils {
    public static final String LABEL_SESSION_DURATION = "session_duration";
    public static final String ACTION_TURN_ON = "aria2_on";
    public static final String ACTION_TURN_OFF = "aria2_off";
    public static final String ACTION_OPENED_ARIA2APP = "opened_aria2app";
    public static final String EVENT_UNKNOWN_LOG_LINE = "read_unknown_log_line";
    public static final String LABEL_LOG_LINE = "log_line";

    public static String optionsBuilder(@Nullable Map<String, String> options) {
        if (options == null || options.isEmpty()) return "";
        StringBuilder extended = new StringBuilder();

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            extended.append(" --").append(entry.getKey()).append("=").append(entry.getValue());
        }

        return extended.toString();
    }

    public static Map<String, String> optionsParser(String options) {
        Map<String, String> map = new HashMap<>();
        String[] lines = options.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#")) continue;
            String[] split = line.split("=");
            map.put(split[0], split.length == 1 ? null : split[1]);
        }
        return map;
    }

    public static void toMap(JSONObject obj, Map<String, String> map) throws JSONException {
        Iterator<String> iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            map.put(key, obj.optString(key, null));
        }
    }

    public static void toJSONObject(JSONObject obj, Map<String, String> map) throws JSONException {
        for (Map.Entry<String, String> entry : map.entrySet())
            obj.put(entry.getKey(), entry.getValue());
    }

    @SuppressWarnings("WeakerAccess")
    public static class Messages {
        public static final Toaster.Message FAILED_CREATING_SESSION_FILE = new Toaster.Message(R.string.failedCreatingSessionFile, true);
        public static final Toaster.Message WRITE_STORAGE_DENIED = new Toaster.Message(R.string.writePermissionDenied, true);
        public static final Toaster.Message CANT_DELETE_BIN = new Toaster.Message(R.string.cannotDeleteBin, true);
        public static final Toaster.Message CANNOT_IMPORT = new Toaster.Message(R.string.cannotImport, true);
        public static final Toaster.Message FILE_NOT_FOUND = new Toaster.Message(R.string.fileNotFound, false);
        public static final Toaster.Message FAILED_LOADING_OPTIONS = new Toaster.Message(R.string.failedLoadingOptions, true);
        public static final Toaster.Message FAILED_SAVING_CUSTOM_OPTIONS = new Toaster.Message(R.string.failedSavingCustomOptions, true);
        public static final Toaster.Message FAILED_STARTING = new Toaster.Message(R.string.failedStarting, true);
        public static final Toaster.Message FAILED_STOPPING = new Toaster.Message(R.string.failedStopping, true);
    }
}