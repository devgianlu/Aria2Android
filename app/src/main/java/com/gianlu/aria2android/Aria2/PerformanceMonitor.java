package com.gianlu.aria2android.Aria2;

import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.RemoteViews;

import com.gianlu.aria2android.PKeys;
import com.gianlu.aria2android.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceMonitor extends Thread {
    private static final Pattern pattern = Pattern.compile("(\\d*?)\\s+(\\d*?)\\s+(\\d*?)%\\s(.)\\s+(\\d*?)\\s+(\\d*?)K\\s+(\\d*?)K\\s+(..)\\s(.*?)\\s+(.*)$");
    private final NotificationManagerCompat manager;
    private final Context context;
    private final int delay;
    private final NotificationCompat.Builder builder;
    private final long startTime;
    private volatile boolean _stop = false;

    public PerformanceMonitor(Context context, NotificationCompat.Builder builder) {
        this.manager = NotificationManagerCompat.from(context);
        this.context = context;
        this.delay = Prefs.getInt(context, PKeys.NOTIFICATION_UPDATE_DELAY, 1);
        this.builder = builder;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        try {
            Process process = Runtime.getRuntime().exec("top -d " + String.valueOf(delay));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null && !_stop) {
                if (line.contains("aria2c")) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find())
                        sendNotification(matcher.group(1), matcher.group(3), matcher.group(7));
                }
            }
        } catch (IOException ex) {
            stopSafe();
            Logging.log(ex);
            builder.setCustomContentView(null);
            manager.notify(BinService.NOTIFICATION_ID, builder.build());
        }
    }

    private void sendNotification(String pid, String cpuUsage, String rss) {
        if (_stop) return;
        RemoteViews layout = new RemoteViews(context.getPackageName(), R.layout.custom_notification);
        layout.setTextViewText(R.id.customNotification_runningTime, "Running time: " + CommonUtils.timeFormatter((System.currentTimeMillis() - startTime) / 1000));
        layout.setTextViewText(R.id.customNotification_pid, "PID: " + pid);
        layout.setTextViewText(R.id.customNotification_cpu, "CPU: " + cpuUsage + "%");
        layout.setTextViewText(R.id.customNotification_memory, "Memory: " + CommonUtils.dimensionFormatter(Integer.parseInt(rss) * 1024, false));
        builder.setCustomContentView(layout);

        manager.notify(BinService.NOTIFICATION_ID, builder.build());
    }

    public void stopSafe() {
        _stop = true;
    }
}
