package com.gianlu.aria2android.Aria2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.gianlu.aria2android.BinUtils;
import com.gianlu.aria2android.BuildConfig;
import com.gianlu.aria2android.MainActivity;
import com.gianlu.aria2android.PK;
import com.gianlu.aria2android.R;
import com.gianlu.aria2android.Utils;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

public class BinService extends Service implements StreamListener.Listener {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_START_SERVICE = "com.gianlu.aria2android.START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.gianlu.aria2android.STOP_SERVICE";
    private static final String CHANNEL_ID = "aria2android";
    private static final String SERVICE_NAME = "aria2 service";
    private final HandlerThread serviceThread = new HandlerThread(SERVICE_NAME);
    private Messenger messenger;
    private Process process;
    private LocalBroadcastManager broadcastManager;
    private StreamListener streamListener;
    private PerformanceMonitor performanceMonitor;
    private ShortcutManager shortcutManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) {
            serviceThread.start();
            broadcastManager = LocalBroadcastManager.getInstance(this);
            messenger = new Messenger(new LocalHandler());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1)
                shortcutManager = (ShortcutManager) getSystemService(Context.SHORTCUT_SERVICE);
        }

        return messenger.getBinder();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, SERVICE_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) service.createNotificationChannel(chan);
    }

    private void startBin(@NonNull StartConfig config) {
        String cmd = BinUtils.createCommandLine(this, config);
        try {
            process = Runtime.getRuntime().exec(cmd);
            streamListener = new StreamListener(process, BinUtils.binVersion(this), this);
            streamListener.start();
        } catch (IOException ex) {
            ex(ex);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                .setContentTitle(SERVICE_NAME)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 5756, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText("aria2c is currently running");

        startForeground(NOTIFICATION_ID, builder.build());
        if (Prefs.getBoolean(PK.SHOW_PERFORMANCE)) {
            performanceMonitor = new PerformanceMonitor(this, builder);
            performanceMonitor.start();
        }

        if (shortcutManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "stopService")
                    .setShortLabel(getString(R.string.stopService))
                    .setIcon(Icon.createWithResource(this, R.drawable.baseline_stop_24))
                    .setIntent(new Intent(ACTION_STOP_SERVICE))
                    .build();

            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
        }

        dispatchBroadcast(Action.SERVER_MSG, new Logging.LogLine(Logging.LogLine.Type.INFO, cmd), null);
        dispatchBroadcast(Action.SERVER_START, null, null);
    }

    private void ex(Exception ex) {
        Logging.log(ex);
        dispatchBroadcast(Action.SERVER_EX, null, ex);
    }

    private void stopBin() {
        try {
            Runtime.getRuntime().exec("pkill aria2c");
        } catch (IOException ex) {
            ex(ex);
        }

        if (process != null) {
            try {
                process.destroy();
                process.waitFor();
                process = null;
            } catch (InterruptedException ex) {
                ex(ex);
            }
        }

        if (streamListener != null) {
            streamListener.stopSafe();
            streamListener = null;
        }

        if (performanceMonitor != null) {
            performanceMonitor.stopSafe();
            streamListener = null;
        }

        if (shortcutManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "startService")
                    .setShortLabel(getString(R.string.startService))
                    .setIcon(Icon.createWithResource(this, R.drawable.baseline_play_arrow_24))
                    .setIntent(new Intent(ACTION_START_SERVICE))
                    .build();

            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
        }

        stopForeground(true);
        dispatchBroadcast(Action.SERVER_STOP, null, null);
    }

    private void dispatchBroadcast(Action action, @Nullable Logging.LogLine msg, @Nullable Throwable ex) {
        Intent intent = new Intent(action.toString());
        if (msg != null) intent.putExtra("msg", msg);
        if (ex != null) intent.putExtra("ex", ex);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onNewLogLine(@NonNull Logging.LogLine line) {
        dispatchBroadcast(Action.SERVER_MSG, line, null);
    }

    @Override
    public void onTerminated() {
        if (process != null) {
            try {
                process.waitFor();
                int exit = process.exitValue();
                if (exit > 0) ex(new Exception("aria2c terminated with exit code " + exit));
                stopBin();
            } catch (InterruptedException | IllegalThreadStateException ex) {
                ex(ex);
            }
        }
    }

    @Override
    public void unknownLogLine(@NonNull String line) {
        if (BuildConfig.DEBUG) System.out.println("UNKNOWN LINE: " + line);

        Bundle bundle = new Bundle();
        bundle.putString(Utils.LABEL_LOG_LINE, line);
        AnalyticsApplication.sendAnalytics(this, Utils.EVENT_UNKNOWN_LOG_LINE, bundle);
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

    @SuppressLint("HandlerLeak")
    private class LocalHandler extends Handler {

        LocalHandler() {
            super(serviceThread.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START:
                    startBin((StartConfig) msg.obj);
                    break;
                case STOP:
                    stopBin();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
