package com.gianlu.aria2android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.gianlu.aria2lib.Aria2Ui;
import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.internal.Message;
import com.gianlu.aria2lib.ui.Aria2ConfigurationScreen;
import com.gianlu.commonutils.FileUtils;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.permissions.AskPermission;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ControlActivityDelegate implements Aria2Ui.Listener {
    static final int STORAGE_ACCESS_CODE = 1;
    private final FragmentActivity context;
    private final UpdateToggle updateToggle;
    private final Aria2ConfigurationScreen screen;
    private final Aria2Ui aria2;

    ControlActivityDelegate(@NonNull FragmentActivity context, @NonNull UpdateToggle updateToggle, @NonNull Aria2ConfigurationScreen screen) throws BadEnvironmentException {
        this.context = context;
        this.updateToggle = updateToggle;
        this.screen = screen;
        this.aria2 = new Aria2Ui(context, this);
        this.aria2.loadEnv(context);
    }

    boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == STORAGE_ACCESS_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    screen.setOutputPathValue(FileUtils.getFullPathFromTreeUri(uri, context));
                    context.getContentResolver().takePersistableUriPermission(uri,
                            data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                }
            }

            return false;
        } else {
            return true;
        }
    }

    void onStart() {
        if (aria2 != null) aria2.bind();
    }

    void onDestroy() {
        if (aria2 != null) aria2.unbind();
    }

    void onResume() {
        if (aria2 != null) aria2.askForStatus();
        if (screen != null) screen.refreshCustomOptionsNumber();
    }

    void toggleService(boolean on) {
        boolean successful;
        if (on) successful = startService();
        else successful = stopService();

        if (successful) updateUiStatus(on);
    }

    private void updateUiStatus(boolean on) {
        if (screen != null) screen.lockPreferences(on);

        updateToggle.setStatus(on);

        if (screen == null && aria2 != null)
            context.runOnUiThread(aria2::askForStatus);
    }

    private boolean startService() {
        Prefs.putLong(PK.CURRENT_SESSION_START, System.currentTimeMillis());
        AnalyticsApplication.sendAnalytics(Utils.ACTION_TURN_ON);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            AskPermission.ask(context, Manifest.permission.WRITE_EXTERNAL_STORAGE, new AskPermission.Listener() {
                @Override
                public void permissionGranted(@NonNull String permission) {
                    toggleService(true);
                }

                @Override
                public void permissionDenied(@NonNull String permission) {
                    Toaster.with(context).message(R.string.writePermissionDenied).error(true).show();
                }

                @Override
                public void askRationale(@NonNull AlertDialog.Builder builder) {
                    builder.setTitle(R.string.permissionRequest)
                            .setMessage(R.string.writeStorageMessage);
                }
            });
            return false;
        }

        File sessionFile = new File(context.getFilesDir(), "session");
        if (Prefs.getBoolean(PK.SAVE_SESSION) && !sessionFile.exists()) {
            try {
                if (!sessionFile.createNewFile()) {
                    Toaster.with(context).message(R.string.failedCreatingSessionFile).error(true).show();
                    return false;
                }
            } catch (IOException ex) {
                Toaster.with(context).message(R.string.failedCreatingSessionFile).ex(ex).show();
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

    private void addLog(@NonNull Logging.LogLine line) {
        Logging.log(line);

        if (screen != null)
            screen.appendLogLine(line);
    }

    @Override
    public void onUpdateLogs(@NonNull List<Aria2Ui.LogMessage> list) {
        for (Aria2Ui.LogMessage msg : list) {
            Logging.LogLine line = createLogLine(msg);
            if (line != null) addLog(line);
        }
    }

    @Nullable
    private Logging.LogLine createLogLine(@NonNull Aria2Ui.LogMessage msg) {
        switch (msg.type) {
            case PROCESS_TERMINATED:
                return new Logging.LogLine(Logging.LogLine.Type.INFO, context.getString(R.string.logTerminated, msg.i));
            case PROCESS_STARTED:
                return new Logging.LogLine(Logging.LogLine.Type.INFO, context.getString(R.string.logStarted, msg.o));
            case MONITOR_FAILED:
            case MONITOR_UPDATE:
                return null;
            case PROCESS_WARN:
                if (msg.o != null)
                    return new Logging.LogLine(Logging.LogLine.Type.WARNING, (String) msg.o);
            case PROCESS_ERROR:
                if (msg.o != null)
                    return new Logging.LogLine(Logging.LogLine.Type.ERROR, (String) msg.o);
            case PROCESS_INFO:
                if (msg.o != null)
                    return new Logging.LogLine(Logging.LogLine.Type.INFO, (String) msg.o);
        }

        return null;
    }

    @Override
    public void onMessage(@NonNull Aria2Ui.LogMessage msg) {
        if (msg.type == Message.Type.MONITOR_FAILED) {
            Logging.log("Monitor failed!", (Throwable) msg.o);
            return;
        }

        if (msg.type == Message.Type.MONITOR_UPDATE) return;

        Logging.LogLine line = createLogLine(msg);
        if (line != null) addLog(line);
    }

    @Override
    public void updateUi(boolean on) {
        updateUiStatus(on);
    }

    @NonNull
    public String version() throws IOException, BadEnvironmentException {
        return aria2.version();
    }

    interface UpdateToggle {
        void setStatus(boolean on);
    }
}
