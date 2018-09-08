package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.gianlu.aria2android.Aria2.BinService;
import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import org.json.JSONException;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        if (Prefs.getBoolean(PK.START_AT_BOOT)) {
            context.getApplicationContext().bindService(new Intent(context, BinService.class), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    Messenger messenger = new Messenger(iBinder);
                    try {
                        messenger.send(Message.obtain(null, BinService.START, StartConfig.fromPrefs()));
                    } catch (RemoteException | JSONException ex) {
                        Logging.log(ex);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                }
            }, Context.BIND_AUTO_CREATE);
        }
    }
}
