package com.gianlu.aria2android;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;

import com.gianlu.aria2android.NetIO.AsyncRequest;
import com.gianlu.aria2android.NetIO.DownloadBinFile;
import com.gianlu.aria2android.NetIO.IResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!BinUtils.binAvailable(this)) {
            downloadBinDialog();
            return;
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
