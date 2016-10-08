package com.gianlu.aria2android;

import android.app.Activity;
import android.widget.Toast;

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

    public static void UIToast(final Activity context, final String text, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static void UIToast(Activity context, String text) {
        UIToast(context, text, Toast.LENGTH_SHORT);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString() + (message.isError() ? " See logs for more..." : ""), Toast.LENGTH_SHORT).show();
            }
        });
        CommonUtils.logMe(context, message.toString(), message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        CommonUtils.logMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        CommonUtils.logMe(context, message + " Details: " + exception.getMessage(), message.isError());
        CommonUtils.secretLog(context, exception);
    }

    public enum TOAST_MESSAGES {
        FAILED_RETRIEVING_RELEASES("Failed retrieving releases!", true),
        FAILED_DOWNLOADING_BIN("Failed downloading bin files!", true),
        FAILED_CREATING_SESSION_FILE("Failed creating an empty session file!", true),
        OUTPUT_PATH_NOT_FOUND("Selected output path cannot be find!", false),
        OUTPUT_PATH_CANNOT_WRITE("Cannot write selected output path!", false),
        WRITE_STORAGE_DENIED("You denied the write permission! Can't start aria2!", true),
        COPIED_TO_CLIPBOARD("Message copied in the clipboard!", false),
        NO_EMAIL_CLIENT("There are no email clients installed.", true),
        LOGS_DELETED("Logs deleted!", false),
        FATAL_EXCEPTION("Fatal exception!", true),
        INVALID_RPC_PORT("Invalid RPC port!", false),
        INVALID_RPC_TOKEN("Invalid RPC token!", false),
        UNEXPECTED_EXCEPTION("Unexpected exception! Don't worry.", true);

        private final String text;
        private final boolean isError;

        TOAST_MESSAGES(final String text, final boolean isError) {
            this.text = text;
            this.isError = isError;
        }

        @Override
        public String toString() {
            return text;
        }

        public boolean isError() {
            return isError;
        }
    }
}
