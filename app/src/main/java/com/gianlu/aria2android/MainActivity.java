package com.gianlu.aria2android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.Interface.Aria2ConfigurationScreen;
import com.gianlu.aria2lib.Interface.DownloadBinActivity;
import com.gianlu.aria2lib.Internal.Message;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.FileUtil;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class MainActivity extends ActivityWithDialog implements Aria2Ui.Listener {
    private static final int STORAGE_ACCESS_CODE = 1;
    private ToggleButton toggleServer;
    private Aria2ConfigurationScreen screen;
    private Aria2Ui aria2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STORAGE_ACCESS_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    screen.setOutputPathValue(FileUtil.getFullPathFromTreeUri(uri, this));
                    getContentResolver().takePersistableUriPermission(uri,
                            data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (aria2 != null) aria2.bind();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aria2 != null) aria2.unbind();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (aria2 != null) aria2.askForStatus();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CommonUtils.isARM() && !Prefs.getBoolean(PK.CUSTOM_BIN)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.archNotSupported)
                    .setMessage(R.string.archNotSupported_message)
                    .setOnDismissListener(dialog -> finish())
                    .setOnCancelListener(dialog -> finish())
                    .setNeutralButton(R.string.importBin, (dialog, which) -> startDownloadBin(true))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> finish());

            showDialog(builder);
            return;
        }

        try {
            aria2 = new Aria2Ui(this, this);
            aria2.loadEnv();
        } catch (BadEnvironmentException ex) {
            Logging.log(ex);
            startDownloadBin(false);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        screen = findViewById(R.id.main_preferences);
        screen.setup(new Aria2ConfigurationScreen.OutputPathSelector(this, STORAGE_ACCESS_CODE), PK.START_AT_BOOT, true);

        toggleServer = findViewById(R.id.main_toggleServer);
        toggleServer.setOnCheckedChangeListener((buttonView, isChecked) -> toggleService(isChecked));

        TextView version = findViewById(R.id.main_binVersion);
        try {
            version.setText(aria2.version());
        } catch (BadEnvironmentException | IOException ex) {
            version.setText(R.string.unknown);
            Logging.log(ex);
        }

        if (Prefs.getBoolean(PK.IS_NEW_BUNDLED_WITH_ARIA2APP, true)) {
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(R.string.useNewAria2AppInstead)
                    .setMessage(R.string.useNewAria2AppInstead_message)
                    .setNeutralButton(android.R.string.ok, null));

            Prefs.putBoolean(PK.IS_NEW_BUNDLED_WITH_ARIA2APP, false);
        }
    }

    private void toggleService(boolean on) {
        boolean successful;
        if (on) successful = startService();
        else successful = stopService();

        if (successful) updateUiStatus(on);
    }

    private void updateUiStatus(boolean on) {
        if (screen != null) screen.lockPreferences(on);

        if (toggleServer != null) {
            toggleServer.setOnCheckedChangeListener(null);
            toggleServer.setChecked(on);
            toggleServer.setOnCheckedChangeListener((buttonView, isChecked) -> toggleService(isChecked));
        }

        if ((screen == null || toggleServer == null) && aria2 != null)
            runOnUiThread(aria2::askForStatus);
    }

    private boolean startService() {
        Prefs.putLong(PK.CURRENT_SESSION_START, System.currentTimeMillis());
        AnalyticsApplication.sendAnalytics(Utils.ACTION_TURN_ON);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            AskPermission.ask(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
                @Override
                public void permissionGranted(@NonNull String permission) {
                    toggleService(true);
                }

                @Override
                public void permissionDenied(@NonNull String permission) {
                    Toaster.with(MainActivity.this).message(R.string.writePermissionDenied).error(true).show();
                }

                @Override
                public void askRationale(@NonNull AlertDialog.Builder builder) {
                    builder.setTitle(R.string.permissionRequest)
                            .setMessage(R.string.writeStorageMessage);
                }
            });
            return false;
        }

        File sessionFile = new File(getFilesDir(), "session");
        if (Prefs.getBoolean(PK.SAVE_SESSION) && !sessionFile.exists()) {
            try {
                if (!sessionFile.createNewFile()) {
                    Toaster.with(this).message(R.string.failedCreatingSessionFile).error(true).show();
                    return false;
                }
            } catch (IOException ex) {
                Toaster.with(this).message(R.string.failedCreatingSessionFile).ex(ex).show();
                return false;
            }
        }

        aria2.startService();
        return true;
    }

    private boolean stopService() {
        aria2.stopService();

        Bundle bundle = null;
        if (Prefs.getLong(PK.CURRENT_SESSION_START, -1) != -1) {
            bundle = new Bundle();
            bundle.putLong(Utils.LABEL_SESSION_DURATION, System.currentTimeMillis() - Prefs.getLong(PK.CURRENT_SESSION_START, -1));
            Prefs.putLong(PK.CURRENT_SESSION_START, -1);
        }

        AnalyticsApplication.sendAnalytics(Utils.ACTION_TURN_OFF, bundle);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void startDownloadBin(boolean importBin) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("importBin", importBin);

        DownloadBinActivity.startActivity(this,
                getString(com.gianlu.aria2lib.R.string.downloadBin) + " - " + getString(com.gianlu.aria2lib.R.string.app_name),
                MainActivity.class, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK, bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainMenu_preferences:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;
            case R.id.mainMenu_changeBin:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.changeBinVersion)
                        .setMessage(R.string.changeBinVersion_message)
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                            if (aria2.delete()) {
                                startDownloadBin(false);
                                finish();
                            } else {
                                Toaster.with(this).message(R.string.cannotDeleteBin).error(true).show();
                            }
                        }).setNegativeButton(android.R.string.no, null);

                showDialog(builder);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addLog(@NonNull Logging.LogLine line) {
        Logging.log(line);

        if (screen != null)
            screen.appendLogLine(line);
    }

    @Override
    public void onMessage(@NonNull Message.Type type, int i, @Nullable Serializable o) {
        switch (type) {
            case PROCESS_TERMINATED:
                addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.logTerminated, i)));
                updateUiStatus(false);
                break;
            case PROCESS_STARTED:
                addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.logStarted, o)));
                updateUiStatus(true);
                break;
            case MONITOR_FAILED:
                Logging.log("Monitor failed!", (Throwable) o);
                break;
            case MONITOR_UPDATE:
                break;
            case PROCESS_WARN:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.WARNING, (String) o));
                break;
            case PROCESS_ERROR:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.ERROR, (String) o));
                break;
            case PROCESS_INFO:
                if (o != null)
                    addLog(new Logging.LogLine(Logging.LogLine.Type.INFO, (String) o));
                break;
        }
    }

    @Override
    public void updateUi(boolean on) {
        updateUiStatus(on);
    }
}
