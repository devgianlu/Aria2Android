package com.gianlu.aria2android;

import android.content.Context;

import com.gianlu.aria2android.NetIO.StatusCodeException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class BinUtils {
    static void unzipBin(byte[] in, Context context) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(in)))) {
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
    }

    static void downloadBin(final URL url, final IDownload handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        handler.onException(new StatusCodeException(conn.getResponseCode(), conn.getResponseMessage()));
                        return;
                    }

                    InputStream in = conn.getInputStream();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    byte data[] = new byte[4096];
                    int count;
                    while ((count = in.read(data)) != -1) {
                        out.write(data, 0, count);
                    }

                    handler.onDone(out.toByteArray());
                    out.close();
                } catch (Exception ex) {
                    handler.onException(ex);
                }
            }
        }).start();
    }

    static boolean binAvailable(Context context) {
        File file = new File(context.getFilesDir(), "aria2c");
        return file.exists() && !file.isDirectory();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void delete(Context context) {
        File file = new File(context.getFilesDir(), "aria2c");
        file.delete();
    }

    static String binVersion(Context context) {
        try {
            return new BufferedReader(
                    new InputStreamReader(
                            Runtime.getRuntime().exec(new File(context.getFilesDir(), "aria2c").getAbsolutePath() + " -v")
                                    .getInputStream()))
                    .readLine();
        } catch (IOException e) {
            return "Unknown";
        }
    }

    interface IDownload {
        void onDone(byte[] out);

        void onException(Exception ex);
    }
}
