package com.gianlu.aria2android.Aria2;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.gianlu.aria2android.BinUtils;
import com.gianlu.commonutils.Logging;

import java.io.IOException;
import java.util.Objects;

public class BinService extends Service implements StreamListener.IStreamListener {
    public static final int START = 0;
    public static final int STOP = 1;
    private final HandlerThread serviceThread = new HandlerThread("aria2c service");
    private Messenger messenger;
    private Process process;
    private LocalBroadcastManager broadcastManager;
    private StreamListener streamListener;

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

    private void startBin(StartConfig config) {
        try {
            process = Runtime.getRuntime().exec(BinUtils.createCommandLine(this, config)); // FIXME: May fail without noticing
            streamListener = new StreamListener(process.getInputStream(), process.getErrorStream(), this);
        } catch (IOException ex) {
            ex(ex);
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
