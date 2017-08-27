package com.gianlu.aria2android.Aria2;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.gianlu.aria2android.BinUtils;
import com.gianlu.aria2android.MainActivity;
import com.gianlu.aria2android.PKeys;
import com.gianlu.aria2android.R;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public class BinService extends IntentService implements StreamListener.IStreamListener {
    public static final String CONFIG = "config";
    public final static int NOTIFICATION_ID = new Random().nextInt();
    private static final String CHANNEL_ID = "aria2android";
    private Process process;
    private NotificationManager manager;
    private PerformanceMonitor monitor;
    private NotificationCompat.Builder builder;
    private StreamListener streamListener;

    public BinService() {
        super("aria2 service");
    }

    private void killService() {
        try {
            Runtime.getRuntime().exec("pkill aria2c");
            process.waitFor();
        } catch (IOException | InterruptedException ex) {
            dispatchAction(Action.SERVER_EX, ex, null);
        }

        if (process != null) process.destroy();
        if (monitor != null) monitor.stopSafe();
        if (streamListener != null) streamListener.stopSafe();
        dispatchAction(Action.SERVER_STOP, null, null);

        manager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    private void dispatchAction(Action action, @Nullable Throwable ex, @Nullable Logging.LogLine msg) {
        Intent intent = new Intent(action.toString());
        if (ex != null) intent.putExtra("ex", ex);
        if (msg != null) intent.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                .setContentTitle("aria2c service")
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, new Random().nextInt(), new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText("aria2c is currently running");

        startForeground(NOTIFICATION_ID, builder.build());

        onHandleIntent(intent);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            process = Runtime.getRuntime().exec(BinUtils.createCommandLine(this, (StartConfig) intent.getSerializableExtra(CONFIG)));
        } catch (IOException ex) {
            dispatchAction(Action.SERVER_EX, ex, null);
            stopSelf();
            return;
        }

        streamListener = new StreamListener(process.getInputStream(), process.getErrorStream(), this);
        streamListener.start();
        dispatchAction(Action.SERVER_START, null, null);

        if (Prefs.getBoolean(this, PKeys.SHOW_PERFORMANCE, true)) {
            monitor = new PerformanceMonitor(this, manager, builder);
            monitor.start();
        }
    }

    @Override
    public void onDestroy() {
        killService();
        super.onDestroy();
    }

    @Override
    public void onNewLogLine(Logging.LogLine line) {
        dispatchAction(Action.SERVER_MSG, null, line);
    }

    public enum Action {
        SERVER_START,
        SERVER_MSG,
        SERVER_EX,
        SERVER_STOP;

        @Nullable
        public static Action find(Intent intent) {
            for (Action action : values())
                if (Objects.equals(action.toString(), intent.getAction()))
                    return action;

            return null;
        }

        @Override
        public String toString() {
            return "com.gianlu.aria2android." + name();
        }
    }
}
