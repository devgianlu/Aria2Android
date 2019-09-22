package com.gianlu.aria2android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.commonutils.preferences.Prefs;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            return;

        if (Prefs.getBoolean(PK.START_AT_BOOT))
            new Aria2Ui(context, null).startServiceFromReceiver();
    }
}
