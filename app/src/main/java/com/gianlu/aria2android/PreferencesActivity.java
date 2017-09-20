package com.gianlu.aria2android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.gianlu.commonutils.AppCompatPreferenceActivity;
import com.gianlu.commonutils.AppCompatPreferenceFragment;
import com.gianlu.commonutils.BaseAboutFragment;
import com.gianlu.commonutils.LogsActivity;
import com.google.android.gms.analytics.HitBuilders;

import java.util.List;

public class PreferencesActivity extends AppCompatPreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.preferences);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        if (header.iconRes == R.drawable.ic_announcement_black_24dp) {
            startActivity(new Intent(this, LogsActivity.class));
            return;
        }

        super.onHeaderClick(header, position);
    }

    protected boolean isValidFragment(String fragmentName) {
        return AppCompatPreferenceFragment.class.getName().equals(fragmentName)
                || AboutFragment.class.getName().equals(fragmentName);
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

        @Override
        protected void sendAnalytics() {
            ThisApplication.sendAnalytics(getActivity(), new HitBuilders.EventBuilder()
                    .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                    .setAction(ThisApplication.ACTION_DONATE_OPEN)
                    .build());
        }

        @Override
        protected Class getParent() {
            return PreferencesActivity.class;
        }
    }
}
