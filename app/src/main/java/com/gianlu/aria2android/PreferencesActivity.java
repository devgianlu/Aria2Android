package com.gianlu.aria2android;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.LogsActivity;
import com.gianlu.commonutils.Preferences.AppCompatPreferenceActivity;
import com.gianlu.commonutils.Preferences.BaseAboutFragment;

import java.util.List;

public class PreferencesActivity extends AppCompatPreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (header.iconRes == R.drawable.baseline_announcement_24) {
            startActivity(new Intent(this, LogsActivity.class));
            return;
        }

        super.onHeaderClick(header, position);
    }

    public static class AboutFragment extends BaseAboutFragment {
        @Override
        protected int getAppNameRes() {
            return R.string.app_name;
        }

        @NonNull
        @Override
        protected String getPackageName() {
            return "com.gianlu.aria2android";
        }

        @Nullable
        @Override
        protected Uri getOpenSourceUrl() {
            return null;
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }
}
