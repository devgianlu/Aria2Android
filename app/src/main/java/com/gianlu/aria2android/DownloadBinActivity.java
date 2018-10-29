package com.gianlu.aria2android;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2android.DownloadBin.GitHubApi;
import com.gianlu.aria2android.DownloadBin.ReleasesAdapter;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.MessageView;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class DownloadBinActivity extends ActivityWithDialog implements GitHubApi.OnResult<List<GitHubApi.Release>>, ReleasesAdapter.IAdapter, BinUtils.IDownloadAndExtractBin {
    private static final int IMPORT_BIN_CODE = 8;
    private ListView list;
    private TextView progress;
    private LinearLayout loading;
    private MessageView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_bin);
        setTitle(getString(R.string.downloadBin) + " - " + getString(R.string.app_name));

        message = findViewById(R.id.downloadBin_message);
        loading = findViewById(R.id.downloadBin_loading);
        progress = findViewById(R.id.downloadBin_progress);
        list = findViewById(R.id.downloadBin_list);

        progress.setText(R.string.retrievingReleases);
        GitHubApi.get().getReleases("aria2", "aria2", this);

        if (getIntent().getBooleanExtra("importBin", false)) {
            importBin();
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
                importBin();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importBin() {
        AskPermission.ask(this, Manifest.permission.READ_EXTERNAL_STORAGE, new AskPermission.Listener() {
            @Override
            public void permissionGranted(@NonNull String permission) {
                try {
                    startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), getString(R.string.customBin)), IMPORT_BIN_CODE);
                } catch (ActivityNotFoundException ex) {
                    Toaster.with(DownloadBinActivity.this).message(R.string.failedImportingBin).ex(ex).show();
                }
            }

            @Override
            public void permissionDenied(@NonNull String permission) {
                Toaster.with(DownloadBinActivity.this).message(R.string.readPermissionDenied).error(true).show();
            }

            @Override
            public void askRationale(@NonNull AlertDialog.Builder builder) {
                builder.setTitle(R.string.readPermission)
                        .setMessage(R.string.readStorage_message);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_BIN_CODE && resultCode == RESULT_OK && data.getData() != null) {
            try {
                BinUtils.writeStreamAsBin(this, getContentResolver().openInputStream(data.getData()));
            } catch (IOException | NetworkOnMainThreadException ex) {
                Toaster.with(this).message(R.string.failedImportingBin).ex(ex).show();
                return;
            }

            AnalyticsApplication.sendAnalytics(Utils.ACTION_IMPORT_BIN);
            Prefs.putBoolean(PK.CUSTOM_BIN, true);

            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResult(@NonNull List<GitHubApi.Release> result) {
        loading.setVisibility(View.GONE);
        message.hide();
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

        Prefs.putBoolean(PK.CUSTOM_BIN, false);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public void onException(@NonNull Exception ex) {
        loading.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
        message.setError(R.string.failedRetrievingReleases_reason, ex.getMessage());
    }

    @Override
    public void onReleaseSelected(GitHubApi.Release release) {
        progress.setText(R.string.downloadingBin);
        loading.setVisibility(View.VISIBLE);
        list.setVisibility(View.GONE);
        message.hide();

        BinUtils.downloadAndExtractBin(this, release.androidAsset, this);
    }
}
