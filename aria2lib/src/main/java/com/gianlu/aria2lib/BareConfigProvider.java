package com.gianlu.aria2lib;

import android.app.Activity;

import androidx.annotation.NonNull;

public interface BareConfigProvider {
    int launcherIcon();

    int notificationIcon();

    @NonNull
    Class<? extends Activity> actionClass();
}
