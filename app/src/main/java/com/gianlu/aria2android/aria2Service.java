package com.gianlu.aria2android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.gianlu.aria2android.aria2.IAria2;
import com.gianlu.aria2android.aria2.aria2StartConfig;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class aria2Service extends IntentService {
    public static final String CONFIG = "config";
    public static IAria2 handler;
    private final int NOTIFICATION_ID = new Random().nextInt();
    private Process process;
    private PerformanceMonitor monitor;
    private NotificationCompat.Builder builder;

    public aria2Service() {
        super("aria2 service");
    }

    private void killService() {
        try {
            Runtime.getRuntime().exec("pkill aria2c");
            process.waitFor();
        } catch (IOException | InterruptedException ex) {
            if (handler != null)
                handler.onException(ex);
        }

        if (process != null)
            process.destroy();

        if (monitor != null)
            monitor.stop();

        if (handler != null)
            handler.onServerStopped();

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        builder = new NotificationCompat.Builder(getBaseContext())
                .setContentTitle("aria2 service")
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(PendingIntent.getActivity(this, new Random().nextInt(), new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText("aria2 is currently running");

        startForeground(NOTIFICATION_ID, builder.build());

        onHandleIntent(intent);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        aria2StartConfig config = (aria2StartConfig) intent.getSerializableExtra(CONFIG);

        String binPath = new File(getFilesDir(), "aria2c").getAbsolutePath();
        String sessionPath = new File(getFilesDir(), "session").getAbsolutePath();

        String command = binPath
                + " --daemon --check-certificate=false"
                + (config.useConfig ? " --conf-path=" + config.configFile : " --no-conf=true")
                + (config.saveSession ? " --save-session=" + sessionPath + " --save-session-interval=10" : " ")
                + " --input-file=" + sessionPath
                + " --dir=" + config.outputDirectory
                + " --enable-rpc --rpc-listen-all=true --rpc-listen-port=" + config.rpcPort
                + " --rpc-secret=" + config.rpcToken
                + Utils.optionProcessor(config.options);

        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            if (handler != null)
                handler.onException(ex);
            stopSelf();
            return;
        }

        if (handler != null)
            handler.onServerStarted(process.getInputStream(), process.getErrorStream());

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Utils.PREF_SHOW_PERFORMANCE, true)) {
            monitor = new PerformanceMonitor(
                    this,
                    PreferenceManager.getDefaultSharedPreferences(this).getInt(Utils.PREF_NOTIFICATION_UPDATE_DELAY, 1),
                    NOTIFICATION_ID,
                    builder);
            new Thread(monitor).start();
        }
    }

    @Override
    public void onDestroy() {
        killService();
    }
}
