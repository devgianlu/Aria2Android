package com.gianlu.aria2android;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.gianlu.aria2android.Google.UncaughtExceptionHandler;
import com.gianlu.commonutils.CommonUtils;

public class PreferencesActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_pref);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        findPreference("email").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
                i.putExtra(Intent.EXTRA_SUBJECT, "Aria2App");
                i.putExtra(Intent.EXTRA_TEXT, "OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")" +
                        "\nOS API Level: " + android.os.Build.VERSION.SDK_INT +
                        "\nDevice: " + android.os.Build.DEVICE +
                        "\nModel (and Product): " + android.os.Build.MODEL + " (" + android.os.Build.PRODUCT + ")");
                try {
                    startActivity(Intent.createChooser(i, "Send mail to the developer..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    CommonUtils.UIToast(PreferencesActivity.this, Utils.ToastMessages.NO_EMAIL_CLIENT);
                }
                return true;
            }
        });

        findPreference("logs").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PreferencesActivity.this, LogsActivity.class));
                return true;
            }
        });

        try {
            findPreference("app_version").setSummary(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException ex) {
            findPreference("app_version").setSummary(R.string.unknown);
        }
    }
}
