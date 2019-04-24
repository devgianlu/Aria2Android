package com.gianlu.aria2android;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.Interface.DownloadBinActivity;
import com.gianlu.aria2lib.Internal.Message;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.FileUtil;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.MessageView;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.yarolegovich.lovelyuserinput.LovelyInput;
import com.yarolegovich.mp.AbsMaterialPreference;
import com.yarolegovich.mp.AbsMaterialTextValuePreference;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialEditTextPreference;
import com.yarolegovich.mp.MaterialPreferenceCategory;
import com.yarolegovich.mp.MaterialPreferenceScreen;
import com.yarolegovich.mp.MaterialSeekBarPreference;
import com.yarolegovich.mp.MaterialStandardPreference;
import com.yarolegovich.mp.io.MaterialPreferences;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class MainActivity extends ActivityWithDialog implements Aria2Ui.Listener {
    private static final int STORAGE_ACCESS_CODE = 1;
    private static final int MAX_LOG_LINES = 100;
    private volatile boolean isRunning;
    private ToggleButton toggleServer;
    private MaterialEditTextPreference outputPath;
    private MaterialPreferenceCategory generalCategory;
    private MaterialPreferenceCategory rpcCategory;
    private MaterialPreferenceCategory notificationsCategory;
    private LinearLayout logsContainer;
    private MessageView logsMessage;
    private Aria2Ui aria2;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STORAGE_ACCESS_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    outputPath.setValue(FileUtil.getFullPathFromTreeUri(uri, this));
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

        MaterialPreferences.instance().setUserInputModule(new LovelyInput.Builder()
                .addIcon(PK.OUTPUT_DIRECTORY.key(), R.drawable.baseline_folder_24)
                .addTextFilter(PK.OUTPUT_DIRECTORY.key(), R.string.invalidOutputPath, text -> {
                    File path = new File(text);
                    return path.exists() && path.canWrite();
                })
                .addIcon(PK.RPC_PORT.key(), R.drawable.baseline_import_export_24)
                .addTextFilter(PK.RPC_PORT.key(), R.string.invalidPort, text -> {
                    try {
                        int port = Integer.parseInt(text);
                        return port > 0 && port < 65536;
                    } catch (Exception ex) {
                        Logging.log(ex);
                        return false;
                    }
                })
                .addIcon(PK.RPC_TOKEN.key(), R.drawable.baseline_vpn_key_24)
                .addTextFilter(PK.RPC_TOKEN.key(), R.string.invalidToken, text -> !text.isEmpty())
                .addIcon(PK.NOTIFICATION_UPDATE_DELAY.key(), R.drawable.baseline_notifications_24)
                .setTopColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build());

        MaterialPreferenceScreen screen = findViewById(R.id.main_preferences);

        // General
        generalCategory = new MaterialPreferenceCategory(this);
        generalCategory.setTitle(R.string.general);
        screen.addView(generalCategory);

        outputPath = new MaterialEditTextPreference.Builder(this)
                .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_BOTTOM)
                .key(PK.OUTPUT_DIRECTORY.key())
                .defaultValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
                .build();
        outputPath.setTitle(R.string.outputPath);
        outputPath.setOverrideClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, STORAGE_ACCESS_CODE);
                return true;
            } catch (ActivityNotFoundException ex) {
                Toaster.with(this).message(R.string.noOpenTree).ex(ex).show();
                return false;
            }
        });
        generalCategory.addView(outputPath);

        MaterialCheckboxPreference saveSession = new MaterialCheckboxPreference.Builder(this)
                .key(PK.SAVE_SESSION.key())
                .defaultValue(PK.SAVE_SESSION.fallback())
                .build();
        saveSession.setTitle(R.string.saveSession);
        saveSession.setSummary(R.string.saveSession_summary);
        generalCategory.addView(saveSession);

        MaterialCheckboxPreference startAtBoot = new MaterialCheckboxPreference.Builder(this)
                .key(PK.START_AT_BOOT.key())
                .defaultValue(PK.START_AT_BOOT.fallback())
                .build();
        startAtBoot.setTitle(R.string.startServiceAtBoot);
        startAtBoot.setSummary(R.string.startServiceAtBoot_summary);
        generalCategory.addView(startAtBoot);

        MaterialStandardPreference customOptions = new MaterialStandardPreference(this);
        customOptions.setOnClickListener(v -> startActivity(new Intent(this, ConfigEditorActivity.class)));
        customOptions.setTitle(R.string.customOptions);
        generalCategory.addView(customOptions);

        // UI
        MaterialPreferenceCategory uiCategory = new MaterialPreferenceCategory(this);
        uiCategory.setTitle(R.string.ui);
        screen.addView(uiCategory);

        MaterialStandardPreference openAria2App = new MaterialStandardPreference(this);
        openAria2App.setOnClickListener(v -> openAria2App());
        openAria2App.setTitle(R.string.openAria2App);
        openAria2App.setSummary(R.string.openAria2App_summary);
        uiCategory.addView(openAria2App);

        // RPC
        rpcCategory = new MaterialPreferenceCategory(this);
        rpcCategory.setTitle(R.string.rpc);
        screen.addView(rpcCategory);

        MaterialEditTextPreference rpcPort = new MaterialEditTextPreference.Builder(this)
                .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_RIGHT)
                .key(PK.RPC_PORT.key())
                .defaultValue(String.valueOf(PK.RPC_PORT.fallback()))
                .build();
        rpcPort.setTitle(R.string.rpcPort);
        rpcCategory.addView(rpcPort);

        MaterialEditTextPreference rpcToken = new MaterialEditTextPreference.Builder(this)
                .showValueMode(AbsMaterialTextValuePreference.SHOW_ON_RIGHT)
                .key(PK.RPC_TOKEN.key())
                .defaultValue(String.valueOf(PK.RPC_TOKEN.fallback()))
                .build();
        rpcToken.setTitle(R.string.rpcToken);
        rpcCategory.addView(rpcToken);

        MaterialCheckboxPreference allowOriginAll = new MaterialCheckboxPreference.Builder(this)
                .key(PK.RPC_ALLOW_ORIGIN_ALL.key())
                .defaultValue(PK.RPC_ALLOW_ORIGIN_ALL.fallback())
                .build();
        allowOriginAll.setTitle(R.string.accessControlAllowOriginAll);
        allowOriginAll.setSummary(R.string.accessControlAllowOriginAll_summary);
        rpcCategory.addView(allowOriginAll);

        // Notifications
        notificationsCategory = new MaterialPreferenceCategory(this);
        notificationsCategory.setTitle(R.string.notification);
        screen.addView(notificationsCategory);

        MaterialCheckboxPreference showPerformance = new MaterialCheckboxPreference.Builder(this)
                .key(PK.SHOW_PERFORMANCE.key())
                .defaultValue(PK.SHOW_PERFORMANCE.fallback())
                .build();
        showPerformance.setTitle(R.string.showPerformance);
        showPerformance.setSummary(R.string.showPerformance_summary);
        notificationsCategory.addView(showPerformance);

        MaterialSeekBarPreference updateDelay = new MaterialSeekBarPreference.Builder(this)
                .showValue(true).minValue(1).maxValue(5)
                .key(PK.NOTIFICATION_UPDATE_DELAY.key())
                .defaultValue(PK.NOTIFICATION_UPDATE_DELAY.fallback())
                .build();
        updateDelay.setTitle(R.string.updateInterval);
        notificationsCategory.addView(updateDelay);

        screen.setVisibilityController(showPerformance, new AbsMaterialPreference[]{updateDelay}, true);

        // Logs
        MaterialPreferenceCategory logsCategory = new MaterialPreferenceCategory(this);
        logsCategory.setTitle(R.string.logs);
        screen.addView(logsCategory);

        logsMessage = new MessageView(this);
        logsMessage.setInfo(R.string.noLogs);
        logsCategory.addView(logsMessage);
        logsMessage.setVisibility(View.VISIBLE);

        logsContainer = new LinearLayout(this);
        logsContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        logsContainer.setPaddingRelative(pad, 0, pad, 0);
        logsCategory.addView(logsContainer);
        logsContainer.setVisibility(View.GONE);

        MaterialStandardPreference clearLogs = new MaterialStandardPreference(this);
        clearLogs.setOnClickListener(v -> {
            logsContainer.removeAllViews();
            logsContainer.setVisibility(View.GONE);
            logsMessage.setVisibility(View.VISIBLE);
        });
        clearLogs.setTitle(R.string.clearLogs);
        logsCategory.addView(clearLogs);

        toggleServer = findViewById(R.id.main_toggleServer);
        toggleServer.setOnCheckedChangeListener((buttonView, isChecked) -> toggleService(isChecked));

        TextView version = findViewById(R.id.main_binVersion);
        try {
            version.setText(aria2.version());
        } catch (BadEnvironmentException | IOException ex) {
            version.setText(R.string.unknown);
            Logging.log(ex);
        }
    }

    private void toggleService(boolean on) {
        boolean successful;
        if (on) successful = startService();
        else successful = stopService();

        if (successful) updateUiStatus(on);
    }

    private void updateUiStatus(boolean on) {
        int visibility = on ? View.GONE : View.VISIBLE;
        generalCategory.setVisibility(visibility);
        rpcCategory.setVisibility(visibility);
        notificationsCategory.setVisibility(visibility);

        isRunning = on;
        toggleServer.setOnCheckedChangeListener(null);
        toggleServer.setChecked(on);
        toggleServer.setOnCheckedChangeListener((buttonView, isChecked) -> toggleService(isChecked));
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

    private void installAria2App() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.aria2AppNotInstalled)
                .setMessage(R.string.aria2AppNotInstalled_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    try {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.gianlu.aria2app")));
                        } catch (ActivityNotFoundException ex) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.gianlu.aria2app")));
                        }
                    } catch (ActivityNotFoundException ex) {
                        Logging.log(ex);
                    }
                }).setNegativeButton(android.R.string.no, null);

        showDialog(builder);
    }

    private void openAria2App() {
        try {
            getPackageManager().getPackageInfo("com.gianlu.aria2app", 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Logging.log(ex);
            installAria2App();
            return;
        }

        if (isRunning) {
            startAria2App();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.aria2NotRunning)
                    .setMessage(R.string.aria2NotRunning_message)
                    .setPositiveButton(android.R.string.no, null)
                    .setNegativeButton(android.R.string.yes, (dialogInterface, i) -> startAria2App());

            showDialog(builder);
        }
    }

    private void startAria2App() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.gianlu.aria2app");
        if (intent != null) {
            startActivity(intent
                    .putExtra("external", true)
                    .putExtra("port", Prefs.getInt(PK.RPC_PORT))
                    .putExtra("token", Prefs.getString(PK.RPC_TOKEN)));
        }

        AnalyticsApplication.sendAnalytics(Utils.ACTION_OPENED_ARIA2APP);
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

        if (logsContainer != null) {
            logsContainer.setVisibility(View.VISIBLE);
            logsMessage.setVisibility(View.GONE);
            logsContainer.addView(Logging.LogLineAdapter.createLogLineView(getLayoutInflater(), logsContainer, line), logsContainer.getChildCount());
            if (logsContainer.getChildCount() > MAX_LOG_LINES)
                logsContainer.removeViewAt(0);
        }
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
