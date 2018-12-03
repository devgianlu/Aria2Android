package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2android.Aria2.BinService;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;

import org.json.JSONException;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        if (Prefs.getBoolean(PK.START_AT_BOOT)) {
            try {
                BinService.startService(context.getApplicationContext());
            } catch (JSONException ex) {
                Logging.log(ex);
            }
        }
    }
}
