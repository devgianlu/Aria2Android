package com.gianlu.aria2android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamListener implements Runnable {
    private static boolean _shouldStop;
    private InputStream in;
    private InputStream err;

    public StreamListener(InputStream in, InputStream err) {
        this.in = in;
        this.err = err;
    }

    public static void stop() {
        _shouldStop = true;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        BufferedReader ereader = new BufferedReader(new InputStreamReader(err));

        while (!_shouldStop) {
            String line;
            String linee;
            try {
                if ((line = reader.readLine()) != null) {
                    System.out.println("SERVER SAYS: " + line);
                }

                if ((linee = ereader.readLine()) != null) {
                    System.out.println("SERVER ERROR SAYS: " + linee);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}