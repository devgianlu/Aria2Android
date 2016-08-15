package com.gianlu.aria2android;

import android.app.IntentService;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2android.aria2.IIncoming;
import com.gianlu.aria2android.aria2.IOutgoing;

import java.io.IOException;
import java.util.Random;

public class aria2Service extends IntentService implements IIncoming {
    private static IOutgoing handler;
    private Process process;
    private Context context;

    public aria2Service() {
        super("aria2 service");

        context = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle("aria2 service")
                .setShowWhen(false)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("aria2 is currently running");

        startForeground(new Random().nextInt(1000), builder.build());

        try {
            process = Runtime.getRuntime().exec(context.getFilesDir().getPath() + "/bin/aria2c --daemon" + (intent.getBooleanExtra("useConfig", false) ? " --conf-path=./aria2.conf" : "--no-conf=true"));
        } catch (IOException ex) {
            handler.onException(ex, true);
        }
    }

    @Override
    public void killService() {
        process.destroy();
    }
}
