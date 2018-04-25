package com.gianlu.aria2android;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.Toaster;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Utils {
    public static final String LABEL_SESSION_DURATION = "session_duration";
    public static final String ACTION_TURN_ON = "aria2_on";
    public static final String ACTION_TURN_OFF = "aria2_off";
    public static final String ACTION_OPENED_ARIA2APP = "opened_aria2app";
    public static final String EVENT_UNKNOWN_LOG_LINE = "read_unknown_log_line";
    public static final String LABEL_LOG_LINE = "log_line";
    public static final String ACTION_IMPORT_BIN = "imported_bin";

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

    @SuppressWarnings("WeakerAccess")
    public static final class Messages {
        public static final Toaster.Message FAILED_CREATING_SESSION_FILE = new Toaster.Message(R.string.failedCreatingSessionFile, true);
        public static final Toaster.Message WRITE_STORAGE_DENIED = new Toaster.Message(R.string.writePermissionDenied, true);
        public static final Toaster.Message CANT_DELETE_BIN = new Toaster.Message(R.string.cannotDeleteBin, true);
        public static final Toaster.Message CANNOT_IMPORT = new Toaster.Message(R.string.cannotImport, true);
        public static final Toaster.Message FILE_NOT_FOUND = new Toaster.Message(R.string.fileNotFound, false);
        public static final Toaster.Message FAILED_LOADING_OPTIONS = new Toaster.Message(R.string.failedLoadingOptions, true);
        public static final Toaster.Message FAILED_SAVING_CUSTOM_OPTIONS = new Toaster.Message(R.string.failedSavingCustomOptions, true);
        public static final Toaster.Message FAILED_STARTING = new Toaster.Message(R.string.failedStarting, true);
        public static final Toaster.Message FAILED_STOPPING = new Toaster.Message(R.string.failedStopping, true);
        public static final Toaster.Message NO_OPEN_TREE = new Toaster.Message(R.string.noOpenTree, false);
        public static final Toaster.Message FAILED_IMPORTING_BIN = new Toaster.Message(R.string.failedImportingBin, true);
    }
}