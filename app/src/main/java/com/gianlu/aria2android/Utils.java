package com.gianlu.aria2android;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    public static void unzipBin(InputStream in, Context context) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) continue;

                if (ze.getName().endsWith("/aria2c")) {
                    File file = new File(context.getFilesDir().getPath() + "/bin", "aria2c");

                    try (FileOutputStream out = new FileOutputStream(file)) {
                        while ((count = zis.read(buffer)) != -1)
                            out.write(buffer, 0, count);
                        out.close();
                    }
                }
            }
        }
    }

    public static void UIToast(final Activity context, final String text) {
        UIToast(context, text, Toast.LENGTH_SHORT);
    }

    public static void UIToast(final Activity context, final String text, final int duration) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static void UIToast(final Activity context, final String text, final int duration, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
        context.runOnUiThread(extra);
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

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final String message_extras, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message + " Details: " + message_extras, message.isError());
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, final Throwable exception, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);

        LogMe(context, message + " Details: " + exception.getMessage(), message.isError());
        SecretLog(context, exception);
    }

    public static void UIToast(final Activity context, final TOAST_MESSAGES message, Runnable extra) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        context.runOnUiThread(extra);
        LogMe(context, message.toString(), message.isError());
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
        FAILED_DOWNLOADING_BIN("Failed downloading bin files!", true);

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
