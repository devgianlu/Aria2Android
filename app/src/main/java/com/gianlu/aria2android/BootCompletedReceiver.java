package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2android.Aria2.BinService;
import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;

import org.json.JSONException;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED))
            return;

        if (Prefs.getBoolean(context, PKeys.START_AT_BOOT, false)) {
            try {
                context.startService(new Intent(context, BinService.class)
                        .putExtra(BinService.CONFIG, StartConfig.fromPrefs(context)));
            } catch (JSONException ex) {
                Logging.logMe(context, ex);
            }
        }
    }
}
