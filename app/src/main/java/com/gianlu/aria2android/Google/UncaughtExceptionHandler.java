package com.gianlu.aria2android.Google;

import android.app.Activity;

import com.gianlu.aria2android.BuildConfig;
import com.gianlu.aria2android.R;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Activity context;

    public UncaughtExceptionHandler(Activity context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable throwable) {
        if (BuildConfig.DEBUG) throwable.printStackTrace();
        if (!BuildConfig.DEBUG) {
            Tracker tracker = Analytics.getTracker();

            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));

            if (tracker != null)
                tracker.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(String.format(Locale.getDefault(), "Thread %d: %s @@ %s", thread.getId(), thread.getName(), throwable.toString() + "\n" + writer.toString()))
                        .setFatal(true)
                        .build());
        }

        CommonUtils.sendEmail(context, context.getString(R.string.app_name));
    }
}
