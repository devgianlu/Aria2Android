package com.gianlu.aria2android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.preferences.BasePreferenceActivity;
import com.gianlu.commonutils.preferences.BasePreferenceFragment;
import com.gianlu.commonutils.preferences.MaterialAboutPreferenceItem;
import com.yarolegovich.mp.MaterialCheckboxPreference;

import java.util.Collections;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity {
    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Collections.singletonList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class));
    }

    @Override
    protected int getAppIconRes() {
        return R.mipmap.ic_launcher;
    }

    @Override
    protected boolean hasTutorial() {
        return false;
    }

    @Nullable
    @Override
    protected String getOpenSourceUrl() {
        return "https://github.com/devgianlu/Aria2Android";
    }

    @Override
    protected boolean disableOtherDonationsOnGooglePlay() {
        return false;
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.NIGHT_MODE.fallback())
                    .key(PK.NIGHT_MODE.key())
                    .build();
            nightMode.setTitle(R.string.prefs_nightMode);
            nightMode.setSummary(R.string.prefs_nightMode_summary);
            addPreference(nightMode);
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }
}
