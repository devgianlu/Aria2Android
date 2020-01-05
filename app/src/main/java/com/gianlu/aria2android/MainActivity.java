package com.gianlu.aria2android;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen;
import com.gianlu.aria2lib.ui.ConfigEditorActivity;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class MainActivity extends ActivityWithDialog implements ControlActivityDelegate.UpdateToggle {
    private ControlActivityDelegate delegate;
    private FloatingActionButton toggleServer;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomAppBar bar = findViewById(R.id.main_bottomAppBar);
        setSupportActionBar(bar);

        Aria2ConfigurationScreen screen = findViewById(R.id.main_preferences);
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, ControlActivityDelegate.STORAGE_ACCESS_CODE), PK.START_AT_BOOT, PK.START_WITH_APP, true);

        toggleServer = findViewById(R.id.main_toggleServer);
        toggleServer.setOnClickListener(view -> {
            Boolean b = (Boolean) view.getTag();
            if (b == null) {
                view.setTag(false);
                b = false;
            }

            delegate.toggleService(!b);
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

        if (Prefs.getBoolean(PK.IS_NEW_BUNDLED_WITH_ARIA2APP, true)) {
            showDialog(new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.useNewAria2AppInstead)
                    .setMessage(R.string.useNewAria2AppInstead_message)
                    .setNeutralButton(android.R.string.ok, null));

            Prefs.putBoolean(PK.IS_NEW_BUNDLED_WITH_ARIA2APP, false);
        }

        if (Prefs.getBoolean(PK.START_WITH_APP, false))
            delegate.toggleService(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainMenu_preferences:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;
            case R.id.mainMenu_customOptions:
                startActivity(new Intent(this, ConfigEditorActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setStatus(boolean on) {
        if (toggleServer == null) return;
        toggleServer.setTag(on);
        if (on) toggleServer.setImageResource(R.drawable.baseline_stop_24);
        else toggleServer.setImageResource(R.drawable.baseline_play_arrow_24);
    }
}
