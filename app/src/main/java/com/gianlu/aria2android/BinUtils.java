package com.gianlu.aria2android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.aria2android.DownloadBin.GitHubApi;
import com.gianlu.commonutils.Logging;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BinUtils {
    private static final int SAVE_SESSION_INTERVAL = 10; // sec

    public static void downloadAndExtractBin(final Context context, final GitHubApi.Release.Asset asset, final IDownloadAndExtractBin listener) {
        final Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(asset.downloadUrl).openConnection();
                    conn.connect();

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onBinDownloaded();
                            }
                        });
                        try (InputStream in = conn.getInputStream(); ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
                            ZipEntry ze;
                            while ((ze = zis.getNextEntry()) != null) {
                                if (ze.isDirectory()) continue;

                                if (ze.getName().endsWith("/aria2c")) {
                                    int count;
                                    byte[] buffer = new byte[8192];
                                    try (FileOutputStream out = context.openFileOutput("aria2c", Context.MODE_PRIVATE)) {
                                        while ((count = zis.read(buffer)) != -1)
                                            out.write(buffer, 0, count);
                                        out.flush();

                                        Runtime.getRuntime().exec("chmod 711 " + new File(context.getFilesDir(), "aria2c").getAbsolutePath());
                                    }
                                }
                            }
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onBinExtracted();
                            }
                        });

                        conn.disconnect();
                    } else {
                        conn.disconnect();
                        throw new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage());
                    }
                } catch (StatusCodeException | IOException ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(ex);
                        }
                    });
                }
            }
        }).start();
    }

    public static void writeStreamAsBin(Context context, InputStream in) throws IOException {
        if (in == null) throw new IOException(new NullPointerException("InputStream is null!"));

        int count;
        byte[] buffer = new byte[8192];
        try (FileOutputStream out = context.openFileOutput("aria2c", Context.MODE_PRIVATE)) {
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();

            Runtime.getRuntime().exec("chmod 711 " + new File(context.getFilesDir(), "aria2c").getAbsolutePath());
        }
    }

    public static boolean binAvailable(Context context) {
        File file = new File(context.getFilesDir(), "aria2c");
        return file.exists() && !file.isDirectory() && file.canExecute();
    }

    public static boolean delete(Context context) {
        File file = new File(context.getFilesDir(), "aria2c");
        return file.delete();
    }

    @Nullable
    public static String binVersion(Context context) {
        try {
            return new BufferedReader(
                    new InputStreamReader(
                            Runtime.getRuntime().exec(new File(context.getFilesDir(), "aria2c").getAbsolutePath() + " -v")
                                    .getInputStream()))
                    .readLine();
        } catch (IOException ex) {
            Logging.log(ex);
            return null;
        }
    }

    public static String createCommandLine(Context context, @NonNull StartConfig config) {
        String binPath = new File(context.getFilesDir(), "aria2c").getAbsolutePath();
        String sessionPath = new File(context.getFilesDir(), "session").getAbsolutePath();

        Map<String, String> options = new HashMap<>(config.options);
        options.put("daemon", "true");
        options.put("check-certificate", "false");
        options.put("input-file", sessionPath);
        options.put("dir", config.outputDirectory);
        options.put("enable-rpc", "true");
        options.put("rpc-listen-all", "true");
        options.put("rpc-listen-port", String.valueOf(config.rpcPort));
        options.put("rpc-secret", config.rpcToken);

        if (config.saveSession) {
            options.put("save-session", sessionPath);
            if (options.get("save-session-interval") == null)
                options.put("save-session-interval", String.valueOf(SAVE_SESSION_INTERVAL));
        }

        if (config.allowOriginAll) options.put("rpc-allow-origin-all", "true");

        return binPath + Utils.optionsBuilder(options);
    }

    public interface IDownloadAndExtractBin {
        void onBinDownloaded();

        void onBinExtracted();

        void onException(Exception ex);
    }
}
