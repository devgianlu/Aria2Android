package com.gianlu.aria2android;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Toaster;

import java.util.Map;

public class Utils {
    static String optionsParser(@Nullable Map<String, String> options) {
        if (options == null || options.isEmpty()) return "";
        StringBuilder extended = new StringBuilder();

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            extended.append(" --").append(entry.getKey()).append("=").append(entry.getValue());
        }

        return extended.toString();
    }

    @SuppressWarnings("WeakerAccess")
    public static class Messages {
        public static final Toaster.Message FAILED_CREATING_SESSION_FILE = new Toaster.Message(R.string.failedCreatingSessionFile, true);
        public static final Toaster.Message WRITE_STORAGE_DENIED = new Toaster.Message(R.string.writePermissionDenied, true);
        public static final Toaster.Message CANT_DELETE_BIN = new Toaster.Message(R.string.cannotDeleteBin, true);
    }
}