package com.gianlu.aria2android;

import com.gianlu.commonutils.CommonUtils;

import java.util.Map;

public class Utils {
    public static final String PREF_OUTPUT_DIRECTORY = "outputPath";
    public static final String PREF_RPC_PORT = "rpcPort";
    public static final String PREF_RPC_TOKEN = "rpcToken";
    public static final String PREF_SAVE_SESSION = "saveSession";

    public static String optionProcessor(Map<String, String> options) {
        String extended = "";
        if (options == null || options.isEmpty()) return "";

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getValue().isEmpty()) continue;

            extended += " --" + entry.getKey() + "=" + entry.getValue();
        }

        return extended;
    }

    public static class ToastMessages {
        public static final CommonUtils.ToastMessage FAILED_RETRIEVING_RELEASES = new CommonUtils.ToastMessage("Failed retrieving releases!", true);
        public static final CommonUtils.ToastMessage FAILED_DOWNLOADING_BIN = new CommonUtils.ToastMessage("Failed downloading bin files!", true);
        public static final CommonUtils.ToastMessage FAILED_CREATING_SESSION_FILE = new CommonUtils.ToastMessage("Failed creating an empty session file!", true);
        public static final CommonUtils.ToastMessage OUTPUT_PATH_NOT_FOUND = new CommonUtils.ToastMessage("Selected output path cannot be find!", false);
        public static final CommonUtils.ToastMessage OUTPUT_PATH_CANNOT_WRITE = new CommonUtils.ToastMessage("Cannot write selected output path!", false);
        public static final CommonUtils.ToastMessage WRITE_STORAGE_DENIED = new CommonUtils.ToastMessage("You denied the write permission! Can't start aria2!", true);
        public static final CommonUtils.ToastMessage COPIED_TO_CLIPBOARD = new CommonUtils.ToastMessage("Message copied in the clipboard!", false);
        public static final CommonUtils.ToastMessage NO_EMAIL_CLIENT = new CommonUtils.ToastMessage("There are no email clients installed.", true);
        public static final CommonUtils.ToastMessage LOGS_DELETED = new CommonUtils.ToastMessage("Logs deleted!", false);
        public static final CommonUtils.ToastMessage FATAL_EXCEPTION = new CommonUtils.ToastMessage("Fatal exception!", true);
        public static final CommonUtils.ToastMessage INVALID_RPC_PORT = new CommonUtils.ToastMessage("Invalid RPC port!", false);
        public static final CommonUtils.ToastMessage INVALID_RPC_TOKEN = new CommonUtils.ToastMessage("Invalid RPC token!", false);
        public static final CommonUtils.ToastMessage UNEXPECTED_EXCEPTION = new CommonUtils.ToastMessage("Unexpected exception! Don't worry.", true);
    }
}
