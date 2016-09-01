package com.gianlu.aria2android;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
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

    public static TextView fastTextView(Context context, String text) {
        TextView textView = new TextView(context);
        textView.setText(text);

        return textView;
    }

    public static void UIToast(final Activity context, final String text, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString() + (message.isError() ? " See logs for more..." : ""), Toast.LENGTH_SHORT).show();
            }
        });
        LogMe(context, message.toString(), message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        LogMe(context, message + " Details: " + exception.getMessage(), message.isError());
        SecretLog(context, exception);
    }

    public static void SecretLog(Activity context, Throwable exx) {
        exx.printStackTrace();

        try {
            FileOutputStream fOut = context.openFileOutput(new SimpleDateFormat("d-LL-yyyy", Locale.getDefault()).format(new java.util.Date()) + ".secret", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write(new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()) + " >> " + exx.toString() + "\n" + Arrays.toString(exx.getStackTrace()) + "\n\n");
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            UIToast(context, "Logger: " + ex.getMessage(), Toast.LENGTH_LONG);
        }
    }

    public static void LogMe(Activity context, String message, boolean isError) {
        try {
            FileOutputStream fOut = context.openFileOutput(new SimpleDateFormat("d-LL-yyyy", Locale.getDefault()).format(new java.util.Date()) + ".log", Context.MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            osw.write((isError ? "--ERROR--" : "--INFO--") + new SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(new java.util.Date()) + " >> " + message.replace("\n", " ") + "\n");
            osw.flush();
            osw.close();
        } catch (IOException ex) {
            UIToast(context, "Logger: " + ex.getMessage(), Toast.LENGTH_LONG);
        }
    }

    public enum TOAST_MESSAGES {
        FAILED_RETRIEVING_RELEASES("Failed retrieving releases!", true),
        FAILED_DOWNLOADING_BIN("Failed downloading bin files!", true),
        FAILED_CREATING_SESSION_FILE("Failed creating an empty session file!", true),
        OUTPUT_PATH_NOT_FOUND("Selected output path cannot be find!", false),
        OUTPUT_PATH_CANNOT_WRITE("Cannot write selected output path!", false),
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
