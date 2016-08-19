package com.gianlu.aria2android;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2android.NetIO.AsyncRequest;
import com.gianlu.aria2android.NetIO.DownloadBinFile;
import com.gianlu.aria2android.NetIO.IResponse;
import com.gianlu.aria2android.aria2.IAria2;
import com.gianlu.aria2android.aria2.aria2StartConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private StreamListener streamListener;
    private boolean isRunning;

    // TODO: Save session
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BinUtils.binAvailable(this)) {
            downloadBinDialog();
            return;
        }

        setContentView(R.layout.activity_main);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        aria2Service.setContext(this);
        aria2Service.handler = new IAria2() {
            @Override
            public void onServerStarted(InputStream in, InputStream err) {
                streamListener = new StreamListener(in, err);
                new Thread(streamListener).start();
            }

            @Override
            public void onException(Exception ex, boolean fatal) {
                StreamListener.stop();
                ex.printStackTrace();
            }

            @Override
            public void onServerStopped() {
                StreamListener.stop();
            }
        };

        TextView version = ((TextView) findViewById(R.id.main_binVersion));
        assert version != null;

        version.setText(BinUtils.binVersion(this));

        ToggleButton toggleServer = (ToggleButton) findViewById(R.id.main_toggleServer);
        assert toggleServer != null;

        final EditText outputPath = (EditText) findViewById(R.id.options_outputPath);
        assert outputPath != null;

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
                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.OUTPUT_PATH_CANNOT_WRITE, path.getAbsolutePath());
                        }
                    } else {
                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.OUTPUT_PATH_NOT_FOUND, path.getAbsolutePath());
                    }
                }
            }
        });

        final EditText rpcPort = (EditText) findViewById(R.id.options_rpcPort);
        assert rpcPort != null;

        rpcPort.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    int port;
                    try {
                        port = Integer.parseInt(rpcPort.getText().toString());
                    } catch (Exception ex) {
                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.INVALID_RPC_PORT, rpcPort.getText().toString());
                        return;
                    }

                    if (port > 0 && port < 65535) {
                        preferences.edit()
                                .putInt(Utils.PREF_RPC_PORT, port)
                                .apply();
                    } else {
                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.INVALID_RPC_PORT, String.valueOf(port));
                    }
                }
            }
        });

        final EditText rpcToken = (EditText) findViewById(R.id.options_rpcToken);
        assert rpcToken != null;

        rpcToken.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (rpcToken.getText().toString().isEmpty()) {
                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.INVALID_RPC_TOKEN);
                    } else {
                        preferences.edit()
                                .putString(Utils.PREF_RPC_TOKEN, rpcToken.getText().toString())
                                .apply();
                    }
                }
            }
        });

        outputPath.setText(preferences.getString(Utils.PREF_OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()));
        rpcPort.setText(String.valueOf(preferences.getInt(Utils.PREF_RPC_PORT, 6800)));
        rpcToken.setText(preferences.getString(Utils.PREF_RPC_TOKEN, "aria2"));

        toggleServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isRunning = isChecked;

                if (isChecked)
                    startService(new Intent(MainActivity.this, aria2Service.class)
                            .putExtra(aria2Service.CONFIG, new aria2StartConfig(
                                    outputPath.getText().toString(),
                                    null,
                                    false,
                                    getPort(outputPath),
                                    rpcToken.getText().toString())
                            ));
                else
                    stopService(new Intent(MainActivity.this, aria2Service.class));


                outputPath.setEnabled(!isChecked);
                rpcToken.setEnabled(!isChecked);
                rpcPort.setEnabled(!isChecked);
            }
        });

        for (ActivityManager.RunningServiceInfo service : ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE))
            if (aria2Service.class.getName().equals(service.service.getClassName()))
                toggleServer.setChecked(true);

        Button openAria2App = (Button) findViewById(R.id.main_openAria2App);
        assert openAria2App != null;

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
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .create().show();

                    return;
                }

                startActivity(getPackageManager().getLaunchIntentForPackage("com.gianlu.aria2app")
                        .putExtra("external", true)
                        .putExtra("port", getPort(rpcPort))
                        .putExtra("token", rpcToken.getText().toString()));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mainMenu_options:
                if (isRunning) {
                    Utils.UIToast(this, Utils.TOAST_MESSAGES.SERVER_RUNNING);
                    break;
                }


                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private int getPort(EditText port) {
        try {
            return Integer.parseInt(port.getText().toString());
        } catch (Exception ex) {
            return 6800;
        }
    }

    private void downloadBinDialog() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setIndeterminate(true);
        pd.setMessage(getString(R.string.loading_releases));

        new Thread(new AsyncRequest(getString(R.string.URL_releases), new IResponse() {
            @Override
            public void onStart() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.show();
                    }
                });
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
                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, ex);
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
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            pd.show();
                                        }
                                    });
                                }

                                @Override
                                public void onResponse(String response) {
                                    pd.dismiss();

                                    String downloadURL;
                                    try {
                                        downloadURL = new JSONObject(response).getJSONArray("assets").getJSONObject(0).getString("browser_download_url");

                                        new DownloadBinFile(MainActivity.this).execute(new URL(downloadURL));
                                    } catch (Exception ex) {
                                        Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, ex);
                                    }
                                }

                                @Override
                                public void onException(Exception exception) {
                                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, exception);
                                }

                                @Override
                                public void onFailed(int code, String message) {
                                    Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, "#" + code + ": " + message);
                                }
                            })).start();
                        } catch (JSONException ex) {
                            Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, ex);
                        }
                    }
                })
                        .setCancelable(false)
                        .setTitle(R.string.whichRelease);

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.create().show();
                    }
                });
            }

            @Override
            public void onException(Exception exception) {
                pd.dismiss();
                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, exception);
            }

            @Override
            public void onFailed(int code, String message) {
                pd.dismiss();
                Utils.UIToast(MainActivity.this, Utils.TOAST_MESSAGES.FAILED_RETRIEVING_RELEASES, "#" + code + ": " + message);
            }
        })).start();
    }
}
