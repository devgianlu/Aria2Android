package com.gianlu.aria2android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2android.Aria2.BinService;
import com.gianlu.aria2android.Aria2.StartConfig;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Prefs;
import com.gianlu.commonutils.Toaster;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static int WRITE_PERMISSION_CODE = 6745;
    private boolean isRunning;
    private ServiceBroadcastReceiver receiver;
    private Logging.LogLineAdapter adapter;
    private RecyclerView logs;
    private TextView noLogs;
    private Messenger serviceMessenger;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            serviceMessenger = new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceMessenger = null;
        }
    };
    private ToggleButton toggleServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BinUtils.binAvailable(this)) {
            startActivity(new Intent(this, DownloadBinActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.permissionRequest)
                        .setMessage(R.string.writeStorageMessage)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_CODE);
                            }
                        });

                CommonUtils.showDialog(this, builder);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_CODE);
            }
        }

        adapter = new Logging.LogLineAdapter(this, new ArrayList<Logging.LogLine>(), null);
        logs = findViewById(R.id.main_logs);
        logs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        logs.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        noLogs = findViewById(R.id.main_noLogs);
        logs.setAdapter(adapter);

        final TextView version = findViewById(R.id.main_binVersion);
        final CheckBox saveSession = findViewById(R.id.options_saveSession);
        final CheckBox startAtBoot = findViewById(R.id.options_startAtBoot);
        toggleServer = findViewById(R.id.main_toggleServer);
        final Button openAria2App = findViewById(R.id.main_openAria2App);
        final SuperEditText outputPath = findViewById(R.id.options_outputPath);
        final SuperEditText rpcPort = findViewById(R.id.options_rpcPort);
        final SuperEditText rpcToken = findViewById(R.id.options_rpcToken);
        final CheckBox allowOriginAll = findViewById(R.id.options_allowOriginAll);
        final CheckBox showPerformance = findViewById(R.id.main_showPerformance);
        final SuperEditText updateDelay = findViewById(R.id.main_updateDelay);
        final Button customOptions = findViewById(R.id.main_customOptions);
        customOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ConfigEditorActivity.class));
            }
        });
        final Button clear = findViewById(R.id.main_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (adapter != null) {
                    adapter.clear();
                    noLogs.setVisibility(View.VISIBLE);
                    logs.setVisibility(View.GONE);
                }
            }
        });

        version.setText(BinUtils.binVersion(this));

        saveSession.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Prefs.putBoolean(MainActivity.this, PKeys.SAVE_SESSION, b);
            }
        });

        startAtBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                Prefs.putBoolean(MainActivity.this, PKeys.START_AT_BOOT, b);
            }
        });

        outputPath.setValidator(new SuperEditText.Validator() {
            @Override
            public void validate(String text) throws SuperEditText.InvalidInputException {
                File path = new File(text);
                if (path.exists()) {
                    if (path.canWrite()) {
                        Prefs.putString(MainActivity.this, PKeys.OUTPUT_DIRECTORY, path.getAbsolutePath());
                    } else {
                        throw new SuperEditText.InvalidInputException(R.string.cannotWriteOutputDirectory);
                    }
                } else {
                    throw new SuperEditText.InvalidInputException(R.string.outputDirectoryDoesNotExist);
                }
            }
        });

        rpcPort.setValidator(new SuperEditText.Validator() {
            @Override
            public void validate(String text) throws SuperEditText.InvalidInputException {
                int port;
                try {
                    port = Integer.parseInt(text);
                } catch (Exception ex) {
                    throw new SuperEditText.InvalidInputException(R.string.invalidPort);
                }

                if (port > 1024 && port < 65535)
                    Prefs.putInt(MainActivity.this, PKeys.RPC_PORT, port);
                else throw new SuperEditText.InvalidInputException(R.string.invalidPort);
            }
        });

        rpcToken.setValidator(new SuperEditText.Validator() {
            @Override
            public void validate(String text) throws SuperEditText.InvalidInputException {
                if (text.isEmpty())
                    throw new SuperEditText.InvalidInputException(R.string.invalidToken);
                else Prefs.putString(MainActivity.this, PKeys.RPC_TOKEN, text);
            }
        });

        allowOriginAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Prefs.putBoolean(MainActivity.this, PKeys.RPC_ALLOW_ORIGIN_ALL, b);
            }
        });

        showPerformance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                Prefs.putBoolean(MainActivity.this, PKeys.SHOW_PERFORMANCE, b);
                updateDelay.setEnabled(b);
            }
        });

        updateDelay.setValidator(new SuperEditText.Validator() {
            @Override
            public void validate(String text) throws SuperEditText.InvalidInputException {
                int delay;
                try {
                    delay = Integer.parseInt(text);
                } catch (Exception ex) {
                    throw new SuperEditText.InvalidInputException(R.string.invalidUpdateDelay);
                }

                if (delay > 0)
                    Prefs.putInt(MainActivity.this, PKeys.NOTIFICATION_UPDATE_DELAY, delay);
                else throw new SuperEditText.InvalidInputException(R.string.invalidUpdateDelay);
            }
        });

        outputPath.setText(Prefs.getString(this, PKeys.OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        saveSession.setChecked(Prefs.getBoolean(this, PKeys.SAVE_SESSION, true));
        startAtBoot.setChecked(Prefs.getBoolean(this, PKeys.START_AT_BOOT, false));
        rpcPort.setText(String.valueOf(Prefs.getInt(this, PKeys.RPC_PORT, 6800)));
        rpcToken.setText(Prefs.getString(this, PKeys.RPC_TOKEN, "aria2"));
        allowOriginAll.setChecked(Prefs.getBoolean(this, PKeys.RPC_ALLOW_ORIGIN_ALL, false));
        showPerformance.setChecked(Prefs.getBoolean(this, PKeys.SHOW_PERFORMANCE, true));
        updateDelay.setText(String.valueOf(Prefs.getInt(this, PKeys.NOTIFICATION_UPDATE_DELAY, 1)));

        toggleServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isRunning = isChecked;

                boolean successful;
                if (isChecked) successful = startService();
                else successful = stopService();

                if (successful) {
                    outputPath.setEnabled(!isChecked);
                    customOptions.setEnabled(!isChecked);
                    saveSession.setEnabled(!isChecked);
                    startAtBoot.setEnabled(!isChecked);
                    rpcToken.setEnabled(!isChecked);
                    rpcPort.setEnabled(!isChecked);
                    allowOriginAll.setEnabled(!isChecked);
                    showPerformance.setEnabled(!isChecked);
                    clear.setEnabled(!isChecked);
                    if (isChecked) updateDelay.setEnabled(false);
                    else updateDelay.setEnabled(showPerformance.isChecked());
                }
            }
        });

        openAria2App.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAria2App();
            }
        });

        // Backward compatibility
        if (Prefs.getBoolean(this, PKeys.DEPRECATED_USE_CONFIG, false)) {
            File file = new File(Prefs.getString(this, PKeys.DEPRECATED_CONFIG_FILE, ""));
            if (file.exists() && file.isFile() && file.canRead()) {
                startActivity(new Intent(this, ConfigEditorActivity.class)
                        .putExtra("import", file.getAbsolutePath()));
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            unbindService(serviceConnection);
        } catch (IllegalArgumentException ignored) {}
        super.onDestroy();
    }

    private boolean startService() {
        Prefs.putLong(MainActivity.this, PKeys.CURRENT_SESSION_START, System.currentTimeMillis());
        ThisApplication.sendAnalytics(MainActivity.this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_TURN_ON)
                .build());

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toaster.show(MainActivity.this, Utils.Messages.WRITE_STORAGE_DENIED);
            return false;
        }

        File sessionFile = new File(getFilesDir(), "session");
        if (Prefs.getBoolean(this, PKeys.SAVE_SESSION, true) && !sessionFile.exists()) {
            try {
                if (!sessionFile.createNewFile()) {
                    Toaster.show(MainActivity.this, Utils.Messages.FAILED_CREATING_SESSION_FILE);
                    return false;
                }
            } catch (IOException ex) {
                Toaster.show(MainActivity.this, Utils.Messages.FAILED_CREATING_SESSION_FILE, ex);
                return false;
            }
        }

        IntentFilter filter = new IntentFilter();
        for (BinService.Action action : BinService.Action.values())
            filter.addAction(action.toString());

        receiver = new ServiceBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        try {
            if (serviceMessenger != null) {
                serviceMessenger.send(Message.obtain(null, BinService.START, StartConfig.fromPrefs(this)));
                return true;
            } else {
                bindService(new Intent(MainActivity.this, BinService.class), serviceConnection, BIND_AUTO_CREATE);
                return false;
            }
        } catch (JSONException ex) {
            Toaster.show(this, Utils.Messages.FAILED_LOADING_OPTIONS, ex);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            return false;
        } catch (RemoteException ex) {
            Toaster.show(this, Utils.Messages.FAILED_STARTING, ex);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(MainActivity.this, BinService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    private boolean stopService() {
        if (serviceMessenger == null) return true;

        try {
            serviceMessenger.send(Message.obtain(null, BinService.STOP, null));
        } catch (RemoteException ex) {
            Toaster.show(this, Utils.Messages.FAILED_STOPPING, ex);
            return false;
        }

        ThisApplication.sendAnalytics(MainActivity.this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_TURN_OFF)
                .build());

        if (Prefs.getLong(MainActivity.this, PKeys.CURRENT_SESSION_START, -1) != -1) {
            ThisApplication.sendAnalytics(MainActivity.this, new HitBuilders.TimingBuilder()
                    .setCategory(ThisApplication.CATEGORY_TIMING)
                    .setVariable(ThisApplication.LABEL_SESSION_DURATION)
                    .setValue(System.currentTimeMillis() - Prefs.getLong(MainActivity.this, PKeys.CURRENT_SESSION_START, -1))
                    .build());

            Prefs.putLong(this, PKeys.CURRENT_SESSION_START, -1);
        }

        return true;
    }

    private void installAria2App() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.aria2AppNotInstalled)
                .setMessage(R.string.aria2AppNotInstalled_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {

                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.gianlu.aria2app")));
                        } catch (android.content.ActivityNotFoundException ex) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.gianlu.aria2app")));
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null);

        CommonUtils.showDialog(this, builder);
    }

    private void openAria2App() {
        try {
            getPackageManager().getPackageInfo("com.gianlu.aria2app", 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Logging.logMe(this, ex);
            installAria2App();
            return;
        }

        if (isRunning) {
            startAria2App();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.aria2NotRunning)
                    .setMessage(R.string.aria2NotRunning_message)
                    .setPositiveButton(android.R.string.no, null)
                    .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startAria2App();
                        }
                    });

            CommonUtils.showDialog(MainActivity.this, builder);
        }
    }

    private void startAria2App() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.gianlu.aria2app");
        if (intent != null) {
            startActivity(intent
                    .putExtra("external", true)
                    .putExtra("port", Prefs.getInt(this, PKeys.RPC_PORT, 6800))
                    .putExtra("token", Prefs.getString(this, PKeys.RPC_TOKEN, "aria2")));
        }

        ThisApplication.sendAnalytics(MainActivity.this, new HitBuilders.EventBuilder()
                .setCategory(ThisApplication.CATEGORY_USER_INPUT)
                .setAction(ThisApplication.ACTION_OPENED_ARIA2APP)
                .build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainMenu_preferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            case R.id.mainMenu_changeBin:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.changeBinVersion)
                        .setMessage(R.string.changeBinVersion_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (BinUtils.delete(MainActivity.this)) {
                                    startActivity(new Intent(MainActivity.this, DownloadBinActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                    finish();
                                } else {
                                    Toaster.show(MainActivity.this, Utils.Messages.CANT_DELETE_BIN);
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null);

                CommonUtils.showDialog(this, builder);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
            final BinService.Action action = BinService.Action.find(intent);
            if (action != null && intent != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        noLogs.setVisibility(View.GONE);
                        logs.setVisibility(View.VISIBLE);

                        switch (action) {
                            case SERVER_START:
                                adapter.add(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.serverStarted)));
                                break;
                            case SERVER_STOP:
                                adapter.add(new Logging.LogLine(Logging.LogLine.Type.INFO, getString(R.string.serverStopped)));
                                LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(receiver);
                                toggleServer.setChecked(false);
                                break;
                            case SERVER_EX:
                                Exception ex = (Exception) intent.getSerializableExtra("ex");
                                Logging.logMe(MainActivity.this, ex);
                                adapter.add(new Logging.LogLine(Logging.LogLine.Type.ERROR, getString(R.string.serverException, ex.getMessage())));
                                break;
                            case SERVER_MSG:
                                adapter.add((Logging.LogLine) intent.getSerializableExtra("msg"));
                                break;
                        }
                    }
                });
            }
        }
    }
}
