package com.gianlu.aria2android;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceMonitor implements Runnable {
    private static final Pattern pattern = Pattern.compile("(\\d*?)\\s+(\\d*?)\\s+(\\d*?)%\\s(.)\\s+(\\d*?)\\s+(\\d*?)K\\s+(\\d*?)K\\s+(..)\\s(.*?)\\s+(.*)$");
    private final Context context;
    private final NotificationManager manager;
    private final int NOTIFICATION_ID = new Random().nextInt();
    private boolean _stop = false;
    private Notification.Builder defaultBuilder;

    public PerformanceMonitor(Context context) {
        this.context = context;
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void run() {
        while (!_stop) {
            try {
                Process process = Runtime.getRuntime().exec("top -n 1"); // TODO: Moved this outside of the loop
                process.waitFor();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("aria2c")) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            sendNotification(matcher.group(1), matcher.group(3), matcher.group(6), matcher.group(7));
                        }
                    }
                }
            } catch (IOException | InterruptedException ignored) {
                // TODO: Remove notification
            }

            try {
                Thread.sleep(1000); // TODO: Adjustable
            } catch (InterruptedException ignored) {
            }
        }
    }

    private Notification.Builder getDefaultBuilder() {
        if (defaultBuilder == null) {
            defaultBuilder = new Notification.Builder(context);
        }

        return defaultBuilder;
    }

    private void sendNotification(String pid, String cpuUsage, String vss, String rss) {
        System.out.println("PID: " + pid + "   CPU: " + cpuUsage + "%   VSS: " + vss + "K   RSS: " + rss + "K");
        /*
        Notification.Builder builder = getDefaultBuilder();
        builder.setContentText("PID: " + pid + "   CPU: " + cpuUsage + "%   VSS: " + vss + "K   RSS: " + rss + "K");
        manager.notify(NOTIFICATION_ID, builder.build());
        */
    }

    public void stop() {
        _stop = true;
    }
}
