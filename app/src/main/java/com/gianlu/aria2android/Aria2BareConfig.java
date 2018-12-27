package com.gianlu.aria2android;

import android.app.Activity;

import com.gianlu.aria2lib.BareConfigProvider;

import androidx.annotation.NonNull;

public final class Aria2BareConfig implements BareConfigProvider {

    public Aria2BareConfig() {
    }

    @Override
    public int launcherIcon() {
        return R.mipmap.ic_launcher;
    }

    @Override
    public int notificationIcon() {
        return R.drawable.ic_notification;
    }

    @NonNull
    @Override
    public Class<? extends Activity> actionClass() {
        return MainActivity.class;
    }
}
