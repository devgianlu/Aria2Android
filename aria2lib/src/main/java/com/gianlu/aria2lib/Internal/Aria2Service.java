package com.gianlu.aria2lib.Internal;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public final class Aria2Service extends Service implements Aria2.MessageListener {
    public static final String ACTION_START_SERVICE = Aria2Service.class.getCanonicalName() + ".START";
    public static final String ACTION_STOP_SERVICE = Aria2Service.class.getCanonicalName() + ".STOP";
    public static final String BROADCAST_MESSAGE = Aria2Service.class.getCanonicalName() + ".BROADCAST_MESSAGE";
    public static final String BROADCAST_STATUS = Aria2Service.class.getCanonicalName() + ".BROADCAST_STATUS";
    public static final int MESSAGE_STATUS = 2;
    private static final String CHANNEL_ID = "aria2service";
    private static final String SERVICE_NAME = "Service for aria2";
    private static final int NOTIFICATION_ID = 4;
    private final HandlerThread serviceThread = new HandlerThread("aria2-service");
    private Messenger messenger;
    private LocalBroadcastManager broadcastManager;
    private Aria2 aria2;
    private NotificationCompat.Builder defaultNotification;
    private NotificationManager notificationManager;
    private long startTime = System.currentTimeMillis();
    private int mLauncherIcon;

    public static void startService(@NonNull Context context, @DrawableRes int launcher, @DrawableRes int notification, @NonNull Class<? extends Activity> actionClass) {
        ContextCompat.startForegroundService(context, new Intent(context, Aria2Service.class)
                .putExtra("notification", notification)
                .putExtra("launcher", launcher)
                .putExtra("actionClass", actionClass)
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
        }

        return messenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aria2 != null) aria2.removeListener(this);
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
                    mLauncherIcon = intent.getIntExtra("launcher", 0);
                    defaultNotification = new NotificationCompat.Builder(getBaseContext(), CHANNEL_ID)
                            .setContentTitle(SERVICE_NAME)
                            .setShowWhen(false)
                            .setAutoCancel(false)
                            .setOngoing(true)
                            .setSmallIcon(intent.getIntExtra("notification", 0))
                            .setContentIntent(PendingIntent.getActivity(this, 2, new Intent(this, (Class<?>) intent.getSerializableExtra("actionClass")), PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText("aria2c is currently running");

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
        layout.setImageViewResource(R.id.customNotification_icon, mLauncherIcon);
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

    private void dispatchStatus() {
        if (broadcastManager == null) return;

        Intent intent = new Intent(BROADCAST_STATUS);
        intent.putExtra("on", aria2.isRunning());
        broadcastManager.sendBroadcast(intent);
    }

    private static class LocalHandler extends Handler {
        private final Aria2Service service;

        LocalHandler(@NonNull Aria2Service service) {
            super(service.serviceThread.getLooper());
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATUS:
                    service.dispatchStatus();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
