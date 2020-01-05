package com.gianlu.aria2android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.Prefs;

import java.io.IOException;

public class TvActivity extends FragmentActivity implements ControlActivityDelegate.UpdateToggle {
    private ControlActivityDelegate delegate;
    private ToggleButton toggleServer;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (delegate.onActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        delegate.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        delegate.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        delegate.onResume();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_main);

        Aria2ConfigurationScreen screen = findViewById(R.id.main_preferences);
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, ControlActivityDelegate.STORAGE_ACCESS_CODE), PK.START_AT_BOOT, PK.START_WITH_APP, true);

        toggleServer = findViewById(R.id.main_toggleServer);
        toggleServer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            delegate.toggleService(isChecked);
        });

        try {
            delegate = new ControlActivityDelegate(this, this, screen);
        } catch (BadEnvironmentException ex) {
            Logging.log(ex);
            finish();
            return;
        }

        TextView version = findViewById(R.id.main_version);
        try {
            version.setText(delegate.version());
        } catch (BadEnvironmentException | IOException ex) {
            version.setText(R.string.unknown);
            Logging.log(ex);
        }

        if (Prefs.getBoolean(PK.START_WITH_APP, false))
            delegate.toggleService(true);
    }

    @Override
    public void setStatus(boolean on) {
        toggleServer.setTag(on);
        toggleServer.setChecked(on);
    }
}
