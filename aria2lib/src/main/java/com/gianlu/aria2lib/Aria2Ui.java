package com.gianlu.aria2lib;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;

import com.gianlu.aria2lib.Internal.Aria2;
import com.gianlu.aria2lib.Internal.Aria2Service;
import com.gianlu.aria2lib.Internal.Message;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class Aria2Ui {
    private final Aria2 aria2;
    private final Context context;
    private final Listener listener;
    private final LocalBroadcastManager broadcastManager;
    private ServiceBroadcastReceiver receiver;
    private Messenger messenger;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);
            broadcastManager.registerReceiver(receiver = new ServiceBroadcastReceiver(), new IntentFilter(Aria2Service.BROADCAST_MESSAGE));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
            if (receiver != null) broadcastManager.unregisterReceiver(receiver);
        }
    };

    public Aria2Ui(@NonNull Context context, @Nullable Listener listener) {
        this.context = context;
        this.listener = listener;
        this.aria2 = Aria2.get();
        this.broadcastManager = LocalBroadcastManager.getInstance(context);
    }

    private void bind() {
        context.bindService(new Intent(context, Aria2Service.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onStart() {
        bind();
    }

    public void onDestroy() {
        context.unbindService(serviceConnection);
    }

    public void loadEnv(@NonNull File env) throws BadEnvironmentException {
        aria2.loadEnv(env);
    }

    @NonNull
    public String version() throws IOException, BadEnvironmentException {
        return aria2.version();
    }

    public void startService() {
        bind();
        Aria2Service.startService(context);
    }

    public void stopService() {
        Aria2Service.stopService(context);
    }

    public boolean delete() {
        return aria2.delete();
    }

    public interface Listener {
        void onMessage(@NonNull Message.Type type, int i, @Nullable Serializable o);
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Message.Type type = (Message.Type) intent.getSerializableExtra("type");
            int i = intent.getIntExtra("i", 0);
            Serializable o = intent.getSerializableExtra("o");
            if (listener != null) listener.onMessage(type, i, o);
        }
    }
}
