package com.gianlu.aria2android;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;

import com.gianlu.aria2android.aria2.IAria2;
import com.gianlu.aria2android.aria2.aria2StartConfig;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class aria2Service extends IntentService {
    public static final String CONFIG = "config";
    public static IAria2 handler;
    private static Process process;

    public aria2Service() {
        super("aria2 service");
    }

    private static void killService() {
        handler.onServerStopped();
        if (process != null)
            process.destroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(new Random().nextInt(100), new Notification.Builder(getBaseContext()).setContentTitle("aria2 service")
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("aria2 is currently running").build());

        onHandleIntent(intent);
        return START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        aria2StartConfig config = intent.getParcelableExtra(CONFIG);

        String binPath = new File(getFilesDir(), "aria2c").getAbsolutePath();
        String confPath = new File(getFilesDir(), "aria2c.conf").getAbsolutePath();
        String sessionPath = new File(getFilesDir(), "session").getAbsolutePath();

        String command = binPath
                + " --daemon --check-certificate=false"
                + (config.useConfig() ? " --conf-path=" + confPath : " --no-conf=true")
                + (config.isSavingSession() ? " --save-session=" + sessionPath + " --save-session-interval=10" : " ")
                + " --input-file=" + sessionPath
                + " --dir=" + config.getOutputDirectory()
                + " --enable-rpc --rpc-listen-all=true --rpc-listen-port=" + config.getRpcPort()
                + " --rpc-secret=" + config.getRpcToken()
                + Utils.optionProcessor(config.getOptions());

        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            handler.onException(ex, true);
            stopSelf();
            return;
        }

        handler.onServerStarted(process.getInputStream(), process.getErrorStream());
    }

    @Override
    public void onDestroy() {
        killService();
    }
}
