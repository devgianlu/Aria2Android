package com.gianlu.aria2android;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.aria2android.NetIO.GitHubApi;
import com.gianlu.aria2android.NetIO.StatusCodeException;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BinUtils {
    public static void downloadAndExtractBin(final Context context, final GitHubApi.Release.Asset asset, final IDownloadAndExtractBin listener) {
        final Handler handler = new Handler(context.getMainLooper());
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
                                        out.close();

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
            Logging.logMe(context, ex);
            return null;
        }
    }

    public static String createCommandLine(Context context, StartConfig config) {
        String binPath = new File(context.getFilesDir(), "aria2c").getAbsolutePath();
        String sessionPath = new File(context.getFilesDir(), "session").getAbsolutePath();

        StringBuilder builder = new StringBuilder();
        builder.append(binPath)
                .append(" --daemon")
                .append(" --check-certificate=false")
                .append(" --input-file=").append(sessionPath)
                .append(" --dir=").append(config.outputDirectory)
                .append(" --enable-rpc")
                .append(" --rpc-listen-all=true")
                .append(" --rpc-listen-port=").append(config.rpcPort)
                .append(" --rpc-secret=").append(config.rpcToken);

        if (config.saveSession)
            builder.append(" --save-session=").append(sessionPath).append(" --save-session-interval=10");
        else builder.append(" ");

        builder.append(Utils.optionsBuilder(config.options));

        return builder.toString();
    }

    public interface IDownloadAndExtractBin {
        void onBinDownloaded();

        void onBinExtracted();

        void onException(Exception ex);
    }
}
