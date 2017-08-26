package com.gianlu.aria2android;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Toaster;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Utils {
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
            if (!line.startsWith("--") || !line.contains("=")) continue;
            String[] split = line.split("=");
            map.put(split[0].substring(2), split[1]);
        }
        return map;
    }

    public static void toMap(JSONObject obj, Map<String, String> map) throws JSONException {
        Iterator<String> iterator = obj.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            map.put(key, obj.getString(key));
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
    }
}