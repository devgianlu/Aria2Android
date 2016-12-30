package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.gianlu.aria2android.aria2.aria2StartConfig;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED))
            return;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (preferences.getBoolean(Utils.PREF_START_AT_BOOT, false))
            context.startService(new Intent(context, aria2Service.class)
                    .putExtra(aria2Service.CONFIG, new aria2StartConfig(
                            preferences.getString(Utils.PREF_OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                            null,
                            preferences.getBoolean(Utils.PREF_USE_CONFIG, false),
                            preferences.getString(Utils.PREF_CONFIG_FILE, ""),
                            preferences.getBoolean(Utils.PREF_SAVE_SESSION, true),
                            preferences.getInt(Utils.PREF_RPC_PORT, 6800),
                            preferences.getString(Utils.PREF_RPC_TOKEN, "aria2"))
                    ));
    }
}
