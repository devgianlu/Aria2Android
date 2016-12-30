package com.gianlu.aria2android;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2android.Google.Analytics;
import com.gianlu.aria2android.Google.UncaughtExceptionHandler;
import com.gianlu.aria2android.Logging.LoglineAdapter;
import com.gianlu.aria2android.Logging.LoglineItem;
import com.gianlu.aria2android.NetIO.AsyncRequest;
import com.gianlu.aria2android.NetIO.IResponse;
import com.gianlu.aria2android.aria2.IAria2;
import com.gianlu.aria2android.aria2.aria2StartConfig;
import com.gianlu.commonutils.CommonUtils;
import com.google.android.gms.analytics.HitBuilders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// TODO: Options file
public class MainActivity extends AppCompatActivity {
    private StreamListener streamListener;
    private boolean isRunning;

    private static int getPort(EditText port) {
        try {
            return Integer.parseInt(port.getText().toString());
        } catch (Exception ex) {
            return 6800;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CommonUtils.DEBUG = BuildConfig.DEBUG;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

        if (!BinUtils.binAvailable(this)) {
            downloadBinDialog();
            return;
        }

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permissionRequest)
                        .setMessage(R.string.writeStorageMessage)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final LoglineAdapter adapter = new LoglineAdapter(this, new ArrayList<LoglineItem>());
        ((ListView) findViewById(R.id.main_logs)).setAdapter(adapter);

        aria2Service.handler = new IAria2() {
            @Override
            public void onServerStarted(InputStream in, InputStream err) {
                adapter.clear();
                streamListener = new StreamListener(adapter, in, err);
                adapter.addLine(LoglineItem.TYPE.INFO, getString(R.string.serverStarted));
                new Thread(streamListener).start();
            }

            @Override
            public void onException(Exception ex, boolean fatal) {
                StreamListener.stop();
                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.UNEXPECTED_EXCEPTION, ex);
                adapter.addLine(LoglineItem.TYPE.ERROR, getString(R.string.serverException, ex.getMessage()));
            }

            @Override
            public void onServerStopped() {
                StreamListener.stop();
                adapter.addLine(LoglineItem.TYPE.INFO, getString(R.string.serverStopped));
            }
        };

        TextView version = ((TextView) findViewById(R.id.main_binVersion));
        final CheckBox saveSession = (CheckBox) findViewById(R.id.options_saveSession);
        final CheckBox startAtBoot = (CheckBox) findViewById(R.id.options_startAtBoot);
        ToggleButton toggleServer = (ToggleButton) findViewById(R.id.main_toggleServer);
        final Button openAria2App = (Button) findViewById(R.id.main_openAria2App);
        final EditText outputPath = (EditText) findViewById(R.id.options_outputPath);
        final EditText rpcPort = (EditText) findViewById(R.id.options_rpcPort);
        final EditText rpcToken = (EditText) findViewById(R.id.options_rpcToken);

        version.setText(BinUtils.binVersion(this));

        saveSession.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                preferences.edit()
                        .putBoolean(Utils.PREF_SAVE_SESSION, b)
                        .apply();
            }
        });

        startAtBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                preferences.edit()
                        .putBoolean(Utils.PREF_START_AT_BOOT, b)
                        .apply();
            }
        });

        outputPath.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    File path = new File(outputPath.getText().toString());

                    if (path.exists()) {
                        if (path.canWrite()) {
                            preferences.edit()
                                    .putString(Utils.PREF_OUTPUT_DIRECTORY, path.getAbsolutePath())
                                    .apply();
                        } else {
                            CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.OUTPUT_PATH_CANNOT_WRITE, path.getAbsolutePath());
                        }
                    } else {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.OUTPUT_PATH_NOT_FOUND, path.getAbsolutePath());
                    }
                }
            }
        });

        rpcPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    int port;
                    try {
                        port = Integer.parseInt(rpcPort.getText().toString());
                    } catch (Exception ex) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.INVALID_RPC_PORT, rpcPort.getText().toString());
                        return;
                    }

                    if (port > 0 && port < 65535) {
                        preferences.edit()
                                .putInt(Utils.PREF_RPC_PORT, port)
                                .apply();
                    } else {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.INVALID_RPC_PORT, String.valueOf(port));
                    }
                }
            }
        });

        rpcToken.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (rpcToken.getText().toString().isEmpty()) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.INVALID_RPC_TOKEN);
                    } else {
                        preferences.edit()
                                .putString(Utils.PREF_RPC_TOKEN, rpcToken.getText().toString())
                                .apply();
                    }
                }
            }
        });

        outputPath.setText(preferences.getString(Utils.PREF_OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        saveSession.setChecked(preferences.getBoolean(Utils.PREF_SAVE_SESSION, true));
        startAtBoot.setChecked(preferences.getBoolean(Utils.PREF_START_AT_BOOT, false));
        rpcPort.setText(String.valueOf(preferences.getInt(Utils.PREF_RPC_PORT, 6800)));
        rpcToken.setText(preferences.getString(Utils.PREF_RPC_TOKEN, "aria2"));

        toggleServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isRunning = isChecked;

                if (isChecked) {
                    preferences.edit().putLong("currentSessionStart", System.currentTimeMillis()).apply();

                    if (Analytics.isTrackingAllowed(MainActivity.this))
                        Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_USER_INPUT)
                                .setAction(Analytics.ACTION_TURN_ON)
                                .build());


                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.WRITE_STORAGE_DENIED);
                        return;
                    }

                    File sessionFile = new File(getFilesDir(), "session");
                    if (saveSession.isChecked() && !sessionFile.exists()) {
                        try {
                            if (!sessionFile.createNewFile()) {
                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CREATING_SESSION_FILE);
                                saveSession.setChecked(false);
                                return;
                            }
                        } catch (IOException ex) {
                            CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_CREATING_SESSION_FILE, ex);
                            saveSession.setChecked(false);
                            return;
                        }
                    }

                    startService(new Intent(MainActivity.this, aria2Service.class)
                            .putExtra(aria2Service.CONFIG, new aria2StartConfig(
                                    outputPath.getText().toString(),
                                    null,
                                    false,
                                    saveSession.isChecked(),
                                    getPort(outputPath),
                                    rpcToken.getText().toString())
                            ));
                } else {
                    stopService(new Intent(MainActivity.this, aria2Service.class));

                    if (Analytics.isTrackingAllowed(MainActivity.this)) {
                        Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_USER_INPUT)
                                .setAction(Analytics.ACTION_TURN_OFF)
                                .build());

                        if (preferences.getLong("currentSessionStart", -1) != -1)
                            Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.TimingBuilder()
                                    .setCategory(Analytics.CATEGORY_TIMING)
                                    .setLabel(Analytics.LABEL_SESSION_DURATION)
                                    .setValue(System.currentTimeMillis() - preferences.getLong("currentSessionStart", -1))
                                    .build());
                    }
                }

                outputPath.setEnabled(!isChecked);
                saveSession.setEnabled(!isChecked);
                startAtBoot.setEnabled(!isChecked);
                rpcToken.setEnabled(!isChecked);
                rpcPort.setEnabled(!isChecked);
            }
        });

        for (ActivityManager.RunningServiceInfo service : ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE))
            if (aria2Service.class.getName().equals(service.service.getClassName()))
                toggleServer.setChecked(true);

        openAria2App.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    getPackageManager().getPackageInfo("com.gianlu.aria2app", 0);
                } catch (PackageManager.NameNotFoundException ex) {
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.aria2App_not_installed)
                            .setMessage(R.string.aria2App_not_installed_message)
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
                            .setNegativeButton(android.R.string.no, null)
                            .create().show();
                    return;
                }

                if (isRunning) {
                    startActivity(getPackageManager().getLaunchIntentForPackage("com.gianlu.aria2app")
                            .putExtra("external", true)
                            .putExtra("port", getPort(rpcPort))
                            .putExtra("token", rpcToken.getText().toString()));

                    if (Analytics.isTrackingAllowed(MainActivity.this))
                        Analytics.getDefaultTracker(getApplication()).send(new HitBuilders.EventBuilder()
                                .setCategory(Analytics.CATEGORY_USER_INPUT)
                                .setAction(Analytics.ACTION_OPENED_ARIA2APP)
                                .build());
                } else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(R.string.aria2_notRunning)
                            .setMessage(R.string.aria2_notRunningMessage)
                            .setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            })
                            .setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(getPackageManager().getLaunchIntentForPackage("com.gianlu.aria2app")
                                            .putExtra("external", true)
                                            .putExtra("port", getPort(rpcPort))
                                            .putExtra("token", rpcToken.getText().toString()));
                                }
                            }).create().show();
                }
            }
        });
    }

    private void downloadBinDialog() {
        final ProgressDialog pd = CommonUtils.fastIndeterminateProgressDialog(this, R.string.loading_releases);
        final ProgressDialog pdd = CommonUtils.fastIndeterminateProgressDialog(MainActivity.this, R.string.downloading_bin);

        new Thread(new AsyncRequest(getString(R.string.URL_releases), new IResponse() {
            @Override
            public void onStart() {
                CommonUtils.showDialog(MainActivity.this, pd);
            }

            @Override
            public void onResponse(String response) {
                final JSONArray jReleases;
                List<String> releasesList = new ArrayList<>();

                try {
                    jReleases = new JSONArray(response);

                    for (int c = 0; c < jReleases.length(); c++) {
                        JSONObject _release = jReleases.getJSONObject(c);

                        releasesList.add(_release.optString("name"));
                    }
                } catch (JSONException ex) {
                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, ex);
                    return;
                } finally {
                    pd.dismiss();
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setSingleChoiceItems(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, releasesList), 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        dialog.dismiss();

                        try {
                            new Thread(new AsyncRequest(jReleases.getJSONObject(which).getString("url"), new IResponse() {
                                @Override
                                public void onStart() {
                                    CommonUtils.showDialog(MainActivity.this, pd);
                                }

                                @Override
                                public void onResponse(String response) {
                                    pd.dismiss();

                                    String downloadURL;
                                    try {
                                        downloadURL = new JSONObject(response).getJSONArray("assets").getJSONObject(0).getString("browser_download_url");

                                        CommonUtils.showDialog(MainActivity.this, pdd);
                                        BinUtils.downloadBin(new URL(downloadURL), new BinUtils.IDownload() {
                                            @Override
                                            public void onDone(byte[] out) {
                                                try {
                                                    BinUtils.unzipBin(out, MainActivity.this);
                                                } catch (IOException ex) {
                                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_DOWNLOADING_BIN, ex);
                                                }

                                                pdd.dismiss();
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        recreate();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onException(Exception ex) {
                                                pdd.dismiss();
                                                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_DOWNLOADING_BIN, ex);
                                            }
                                        });
                                    } catch (Exception ex) {
                                        CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, ex);
                                    }
                                }

                                @Override
                                public void onException(Exception exception) {
                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, exception);
                                }

                                @Override
                                public void onFailed(int code, String message) {
                                    CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, "#" + code + ": " + message);
                                }
                            })).start();
                        } catch (JSONException ex) {
                            CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, ex);
                        }
                    }
                })
                        .setCancelable(false)
                        .setTitle(R.string.whichRelease);

                CommonUtils.showDialog(MainActivity.this, builder);
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, exception);
            }

            @Override
            public void onFailed(int code, String message) {
                pd.dismiss();
                CommonUtils.UIToast(MainActivity.this, Utils.ToastMessages.FAILED_RETRIEVING_RELEASES, "#" + code + ": " + message);
            }
        })).start();
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
                break;
            case R.id.mainMenu_changeBin:
                if (BinUtils.delete(this))
                    downloadBinDialog();
                else
                    CommonUtils.UIToast(this, Utils.ToastMessages.CANT_DELETE_BIN);
                break;
        }
        return true;
    }
}
