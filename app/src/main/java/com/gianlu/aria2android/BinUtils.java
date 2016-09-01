package com.gianlu.aria2android;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BinUtils {
    public static void unzipBin(InputStream in, Context context) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.isDirectory()) continue;

                if (ze.getName().endsWith("/aria2c")) {
                    File binParent = new File(context.getFilesDir().getPath() + "/bin");
                    Runtime.getRuntime().exec("chmod 777 " + binParent.getPath());
                    if (!binParent.mkdirs()) continue;

                    File file = new File(binParent, "/aria2c");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        while ((count = zis.read(buffer)) != -1)
                            out.write(buffer, 0, count);
                        out.close();

                        Runtime.getRuntime().exec("chmod 777 " + file.getPath());
                    }

                }
            }
        }
    }

    public static boolean binAvailable(Context context) {
        File file = new File(context.getFilesDir().getPath() + "/bin/aria2c");
        return file.exists() && !file.isDirectory();
    }

    public static String binVersion(Context context) {
        try {
            return new BufferedReader(
                    new InputStreamReader(
                            Runtime.getRuntime().exec(context.getFilesDir().getPath() + "/bin/aria2c -v")
                                    .getInputStream()))
                    .readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
