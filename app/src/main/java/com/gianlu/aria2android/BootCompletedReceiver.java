package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.gianlu.aria2android.Aria2.BinService;
import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.commonutils.Prefs;

import java.util.Objects;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED))
            return;

        if (Prefs.getBoolean(context, PKeys.START_AT_BOOT, false))
            context.startService(new Intent(context, BinService.class)
                    .putExtra(BinService.CONFIG, new StartConfig(
                            Prefs.getString(context, PKeys.OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                            null,
                            Prefs.getBoolean(context, PKeys.USE_CONFIG, false),
                            Prefs.getString(context, PKeys.CONFIG_FILE, ""),
                            Prefs.getBoolean(context, PKeys.SAVE_SESSION, true),
                            Prefs.getInt(context, PKeys.RPC_PORT, 6800),
                            Prefs.getString(context, PKeys.RPC_TOKEN, "aria2"))
                    ));
    }
}
