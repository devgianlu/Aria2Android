package com.gianlu.aria2android;

import android.widget.EditText;

import com.gianlu.commonutils.CommonUtils;

import java.util.Map;

public class Utils {
    static final String PREF_OUTPUT_DIRECTORY = "outputPath";
    static final String PREF_RPC_PORT = "rpcPort";
    static final String PREF_RPC_TOKEN = "rpcToken";
    static final String PREF_SAVE_SESSION = "saveSession";
    static final String PREF_START_AT_BOOT = "startAtBoot";
    static final String PREF_USE_CONFIG = "useConfig";
    static final String PREF_CONFIG_FILE = "configFile";

    static String optionProcessor(Map<String, String> options) {
        String extended = "";
        if (options == null || options.isEmpty()) return "";

        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey().isEmpty() || entry.getValue().isEmpty()) continue;

            extended += " --" + entry.getKey() + "=" + entry.getValue();
        }

        return extended;
    }

    public static int getPort(EditText port) {
        try {
            return Integer.parseInt(port.getText().toString());
        } catch (Exception ex) {
            return 6800;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ToastMessages {
        public static final CommonUtils.ToastMessage FAILED_RETRIEVING_RELEASES = new CommonUtils.ToastMessage("Failed retrieving releases!", true);
        public static final CommonUtils.ToastMessage FAILED_DOWNLOADING_BIN = new CommonUtils.ToastMessage("Failed downloading bin files!", true);
        public static final CommonUtils.ToastMessage FAILED_CREATING_SESSION_FILE = new CommonUtils.ToastMessage("Failed creating an empty session file!", true);
        public static final CommonUtils.ToastMessage OUTPUT_PATH_NOT_FOUND = new CommonUtils.ToastMessage("Selected output path doesn't exist!", false);
        public static final CommonUtils.ToastMessage OUTPUT_PATH_CANT_WRITE = new CommonUtils.ToastMessage("Cannot write selected output path!", false);
        public static final CommonUtils.ToastMessage WRITE_STORAGE_DENIED = new CommonUtils.ToastMessage("You denied the write permission! Can't start aria2!", true);
        public static final CommonUtils.ToastMessage COPIED_TO_CLIPBOARD = new CommonUtils.ToastMessage("Message copied in the clipboard!", false);
        public static final CommonUtils.ToastMessage CANT_DELETE_BIN = new CommonUtils.ToastMessage("Can't delete the bin file! Please clear application data.", true);
        public static final CommonUtils.ToastMessage INVALID_RPC_PORT = new CommonUtils.ToastMessage("Invalid RPC port!", false);
        public static final CommonUtils.ToastMessage INVALID_RPC_TOKEN = new CommonUtils.ToastMessage("Invalid RPC token!", false);
        public static final CommonUtils.ToastMessage UNEXPECTED_EXCEPTION = new CommonUtils.ToastMessage("Unexpected exception! Don't worry.", true);
        public static final CommonUtils.ToastMessage CONFIG_FILE_CANT_READ = new CommonUtils.ToastMessage("Selected config file doesn't exist!", false);
        public static final CommonUtils.ToastMessage CONFIG_FILE_NOT_FOUND = new CommonUtils.ToastMessage("Selected config file can't be read!", false);
    }
}