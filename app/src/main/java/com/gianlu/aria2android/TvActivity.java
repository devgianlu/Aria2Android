package com.gianlu.aria2android;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen;
import com.gianlu.commonutils.logging.Logging;

import java.io.IOException;
import java.util.List;

public class TvActivity extends FragmentActivity implements Aria2Ui.Listener {
    private Aria2Ui aria2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_activity_main);

        try {
            aria2 = new Aria2Ui(this, this);
            aria2.loadEnv(this);
        } catch (BadEnvironmentException ex) {
            Logging.log(ex);
            finish();
            return;
        }

        TextView version = findViewById(R.id.main_version);
        try {
            version.setText(aria2.version());
        } catch (BadEnvironmentException | IOException ex) {
            version.setText(R.string.unknown);
            Logging.log(ex);
        }

        Aria2ConfigurationScreen screen = findViewById(R.id.main_preferences);
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, 2), PK.START_AT_BOOT, true);
    }

    @Override
    public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> msg) {
        // TODO
    }

    @Override
    public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
        // TODO
    }

    @Override
    public void updateUi(boolean on) {
        // TODO
    }
}
