package com.gianlu.aria2android;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static boolean binAvailable(Context context) {
        return new File(context.getFilesDir().getPath() + "/bin/aria2c").exists();
    }
}
