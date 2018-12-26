package com.gianlu.aria2lib.Internal;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.widget.RemoteViews;

import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public final class Aria2Service extends Service implements Aria2.MessageListener {
    public static final String ACTION_START_SERVICE = Aria2Service.class.getCanonicalName() + ".START";
    public static final String ACTION_STOP_SERVICE = Aria2Service.class.getCanonicalName() + ".STOP";
    public static final String BROADCAST_MESSAGE = Aria2Service.class.getCanonicalName() + ".BROADCAST_MESSAGE";
    private static final String CHANNEL_ID = "aria2service";
    private static final String SERVICE_NAME = "Service for aria2";
    private static final int NOTIFICATION_ID = 4;
    private final HandlerThread serviceThread = new HandlerThread("aria2android-service");
    private Messenger messenger;
    private LocalBroadcastManager broadcastManager;
    private Aria2 aria2;
    private NotificationCompat.Builder defaultNotification;
    private NotificationManager notificationManager;
    private long startTime = System.currentTimeMillis();

    public static void startService(@NonNull Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, Aria2Service.class)
                .setAction(ACTION_START_SERVICE));
    }

    public static void stopService(@NonNull Context context) {
        context.startService(new Intent(context, Aria2Service.class)
                .setAction(ACTION_STOP_SERVICE));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) {
            serviceThread.start();
            broadcastManager = LocalBroadcastManager.getInstance(this);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            messenger = new Messenger(new LocalHandler(this));
            aria2 = Aria2.get();
            aria2.addListener(this);

            defaultNotification = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                    .setContentTitle(SERVICE_NAME)
                    .setShowWhen(false)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setSmallIcon(android.R.drawable.star_on) // FIXME
                    //   .setContentIntent(PendingIntent.getActivity(this, 2, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentText("aria2c is currently running");
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Objects.equals(intent.getAction(), ACTION_START_SERVICE)) {
                try {
                    start();
                    return START_STICKY;
                } catch (IOException | BadEnvironmentException ex) {
                    Logging.log(ex);
                }
            } else if (Objects.equals(intent.getAction(), ACTION_STOP_SERVICE)) {
                stop();
            }
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    private void stop() {
        aria2.stop();
        stopForeground(true);
    }

    private void start() throws IOException, BadEnvironmentException {
        aria2.start();
        startTime = System.currentTimeMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel();
        startForeground(NOTIFICATION_ID, defaultNotification.build());
    }

    @Override
    public void onMessage(@NonNull com.gianlu.aria2lib.Internal.Message msg) {
        dispatch(msg);

        switch (msg.type()) {
            case MONITOR_UPDATE:
                updateMonitor((MonitorUpdate) msg.object());
                break;
        }
    }

    private void updateMonitor(@Nullable MonitorUpdate update) {
        if (update == null || notificationManager == null) return;

        RemoteViews layout = new RemoteViews(getPackageName(), R.layout.custom_notification);
        layout.setTextViewText(R.id.customNotification_runningTime, "Running time: " + CommonUtils.timeFormatter((System.currentTimeMillis() - startTime) / 1000));
        layout.setTextViewText(R.id.customNotification_pid, "PID: " + update.pid());
        layout.setTextViewText(R.id.customNotification_cpu, "CPU: " + update.cpu() + "%");
        layout.setTextViewText(R.id.customNotification_memory, "Memory: " + CommonUtils.dimensionFormatter(Integer.parseInt(update.rss()) * 1024, false));
        defaultNotification.setCustomContentView(layout);

        notificationManager.notify(NOTIFICATION_ID, defaultNotification.build());

        update.recycle();
    }

    private void dispatch(@NonNull com.gianlu.aria2lib.Internal.Message msg) {
        Intent intent = new Intent(BROADCAST_MESSAGE);
        intent.putExtra("type", msg.type());
        intent.putExtra("i", msg.integer());
        if (msg.object() instanceof Serializable) intent.putExtra("o", (Serializable) msg.object());
        broadcastManager.sendBroadcast(intent);
    }

    private static class LocalHandler extends Handler {
        LocalHandler(@NonNull Aria2Service service) {
            super(service.serviceThread.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
