package com.gianlu.aria2android;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gianlu.aria2android.NetIO.AsyncRequest;
import com.gianlu.aria2android.NetIO.DownloadBinFile;
import com.gianlu.aria2android.NetIO.IResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!BinUtils.binAvailable(this)) {
            downloadBinDialog();
            return;
        }

        setContentView(R.layout.activity_main);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        TextView version = ((TextView) findViewById(R.id.main_binVersion));
        assert version != null;

        version.setText(BinUtils.binVersion(this));

        ToggleButton toggleServer = (ToggleButton) findViewById(R.id.main_toggleServer);
        assert toggleServer != null;

        toggleServer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

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

        outputPath.setText(preferences.getString(Utils.PREF_OUTPUT_DIRECTORY, null) == null ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() : preferences.getString("outputPath", "/"));
        rpcPort.setText(String.valueOf(preferences.getInt(Utils.PREF_RPC_PORT, 6800)));
        rpcToken.setText(preferences.getString(Utils.PREF_RPC_TOKEN, "aria2"));
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
