package com.gianlu.aria2android;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2android.DownloadBin.ReleasesAdapter;
import com.gianlu.aria2android.NetIO.GitHubApi;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.MessageLayout;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import java.io.IOException;
import java.util.List;

public class DownloadBinActivity extends AppCompatActivity implements GitHubApi.IResult<List<GitHubApi.Release>>, ReleasesAdapter.IAdapter, BinUtils.IDownloadAndExtractBin {
    private static final int READ_PERMISSION_CODE = 5;
    private static final int IMPORT_BIN_CODE = 8;
    private ListView list;
    private TextView progress;
    private LinearLayout loading;
    private FrameLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_bin);
        setTitle(getString(R.string.downloadBin) + " - " + getString(R.string.app_name));

        layout = findViewById(R.id.downloadBin);
        loading = findViewById(R.id.downloadBin_loading);
        progress = findViewById(R.id.downloadBin_progress);
        list = findViewById(R.id.downloadBin_list);

        progress.setText(R.string.retrievingReleases);
        GitHubApi.get().getReleases("aria2", "aria2", this);

        if (getIntent().getBooleanExtra("importBin", false)) {
            showCustomBinDialog();
            getIntent().removeExtra("importBin");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download_bin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.downloadBin_custom:
                showCustomBinDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importBin() {
        startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), getString(R.string.customBin)), IMPORT_BIN_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == READ_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            importBin();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_BIN_CODE && resultCode == RESULT_OK && data.getData() != null) {
            try {
                BinUtils.writeStreamAsBin(this, getContentResolver().openInputStream(data.getData()));
            } catch (IOException ex) {
                Toaster.show(this, Utils.Messages.FAILED_IMPORTING_BIN, ex);
                return;
            }

            AnalyticsApplication.sendAnalytics(this, Utils.ACTION_IMPORT_BIN);
            Prefs.putBoolean(this, PKeys.CUSTOM_BIN, true);

            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showCustomBinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.customBin)
                .setMessage(R.string.customBin_message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ContextCompat.checkSelfPermission(DownloadBinActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(DownloadBinActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(DownloadBinActivity.this)
                                        .setTitle(R.string.readPermission)
                                        .setMessage(R.string.readStorage_message)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                ActivityCompat.requestPermissions(DownloadBinActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_CODE);
                                            }
                                        });

                                CommonUtils.showDialog(DownloadBinActivity.this, builder);
                            } else {
                                ActivityCompat.requestPermissions(DownloadBinActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_CODE);
                            }
                        } else {
                            importBin();
                        }
                    }
                })
                .setNegativeButton(R.string.no, null);

        CommonUtils.showDialog(this, builder);
    }

    @Override
    public void onResult(List<GitHubApi.Release> result) {
        loading.setVisibility(View.GONE);
        MessageLayout.hide(layout);
        list.setVisibility(View.VISIBLE);
        list.setAdapter(new ReleasesAdapter(this, result, this));
    }

    @Override
    public void onBinDownloaded() {
        progress.setText(R.string.extractingBin);
    }

    @Override
    public void onBinExtracted() {
        progress.setText(R.string.binExtracted);

        Prefs.putBoolean(this, PKeys.CUSTOM_BIN, false);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public void onException(Exception ex) {
        loading.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        MessageLayout.show(layout, getString(R.string.failedRetrievingReleases_reason, ex.getMessage()), R.drawable.ic_error_outline_black_48dp);
    }

    @Override
    public void onReleaseSelected(GitHubApi.Release release) {
        progress.setText(R.string.downloadingBin);
        loading.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        MessageLayout.hide(layout);

        BinUtils.downloadAndExtractBin(this, release.androidAsset, this);
    }
}
