package com.gianlu.aria2android.Aria2;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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
import com.gianlu.aria2android.PKeys;
import com.gianlu.aria2android.R;
import com.gianlu.aria2android.ThisApplication;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;
import com.google.android.gms.analytics.HitBuilders;

import java.io.IOException;
import java.util.Objects;

public class BinService extends Service implements StreamListener.IStreamListener {
    public static final int START = 0;
    public static final int STOP = 1;
    public static final int NOTIFICATION_ID = 4534532;
    private static final String CHANNEL_ID = "aria2android";
    private final HandlerThread serviceThread = new HandlerThread("aria2c service");
    private Messenger messenger;
    private Process process;
    private LocalBroadcastManager broadcastManager;
    private StreamListener streamListener;
    private PerformanceMonitor performanceMonitor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) {
            serviceThread.start();
            broadcastManager = LocalBroadcastManager.getInstance(this);
            messenger = new Messenger(new LocalHandler());
        }

        return messenger.getBinder();
    }

    private void startBin(@NonNull StartConfig config) {
        try {
            process = Runtime.getRuntime().exec(BinUtils.createCommandLine(this, config));
            streamListener = new StreamListener(process, this);
            streamListener.start();
        } catch (IOException ex) {
            ex(ex);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                .setContentTitle("aria2c service")
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, 5756, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText("aria2c is currently running");

        startForeground(NOTIFICATION_ID, builder.build());
        if (Prefs.getBoolean(this, PKeys.SHOW_PERFORMANCE, true)) {
            performanceMonitor = new PerformanceMonitor(this, builder);
            performanceMonitor.start();
        }

        dispatchBroadcast(Action.SERVER_START, null, null);
    }

    private void ex(Exception ex) {
        Logging.logMe(this, ex);
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
    public void onNewLogLine(Logging.LogLine line) {
        dispatchBroadcast(Action.SERVER_MSG, line, null);
    }

    @Override
    public void onTerminated() {
        if (process != null) {
            try {
                process.waitFor();
                int exit = process.exitValue(); // TODO: Create map of exit codes
                if (exit > 0) ex(new Exception("aria2c terminated with exit code " + exit));
                stopBin();
            } catch (InterruptedException | IllegalThreadStateException ex) {
                ex(ex);
            }
        }
    }

    @Override
    public void unknownLogLine(String line) {
        if (BuildConfig.DEBUG) System.out.println("UNKNOWN LINE: " + line);
        ThisApplication.sendAnalytics(this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_UNKNOWN_LOG_LINE)
                .setAction(ThisApplication.ACTION_UNKNOWN_LOG_LINE)
                .setLabel(line)
                .build());
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
