package com.gianlu.aria2android;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceMonitor implements Runnable {
    private static final Pattern pattern = Pattern.compile("(\\d*?)\\s+(\\d*?)\\s+(\\d*?)%\\s(.)\\s+(\\d*?)\\s+(\\d*?)K\\s+(\\d*?)K\\s+(..)\\s(.*?)\\s+(.*)$");
    private final NotificationManager manager;
    private final int NOTIFICATION_ID;
    private final Notification.Builder builder;
    private boolean _stop = false;

    public PerformanceMonitor(Context context, int NOTIFICATION_ID, Notification.Builder builder) {
        this.manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.NOTIFICATION_ID = NOTIFICATION_ID;
        this.builder = builder;
    }

    @Override
    public void run() {
        try {
            Process process = Runtime.getRuntime().exec("top -d 1"); // TODO: Adjustable delay

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null && !_stop) {
                if (line.contains("aria2c")) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        sendNotification(matcher.group(1), matcher.group(3), matcher.group(6), matcher.group(7));
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            // TODO: Log
        }
    }

    private void sendNotification(String pid, String cpuUsage, String vss, String rss) {
        // System.out.println("PID: " + pid + "   CPU: " + cpuUsage + "%   VSS: " + vss + "K   RSS: " + rss + "K"); TODO: Print to log (maybe)
        builder.setContentText("PID: " + pid + "   CPU: " + cpuUsage + "%   VSS: " + vss + "K   RSS: " + rss + "K"); // TODO: Custom view
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    public void stop() {
        _stop = true;
    }
}
