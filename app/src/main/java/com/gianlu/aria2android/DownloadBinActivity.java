package com.gianlu.aria2android;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.gianlu.aria2lib.Aria2Downloader;
import com.gianlu.aria2lib.GitHubApi;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.AskPermission;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.RecyclerViewLayout;
import com.gianlu.commonutils.Toaster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DownloadBinActivity extends ActivityWithDialog implements ReleasesAdapter.Listener, GitHubApi.OnResult<List<GitHubApi.Release>>, Aria2Downloader.DownloadRelease, Aria2Downloader.ExtractTo {
    private static final int IMPORT_BIN_CODE = 8;
    private RecyclerViewLayout layout;
    private Aria2Downloader downloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new RecyclerViewLayout(this);
        setContentView(layout);
        setTitle(getString(R.string.downloadBin) + " - " + getString(R.string.app_name));

        layout.showInfo(R.string.retrievingReleases);
        downloader = new Aria2Downloader();

        if (getIntent().getBooleanExtra("importBin", false)) {
            importBin();
            getIntent().removeExtra("importBin");
        } else {
            downloader.getReleases(this);
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

    public void writeStreamAsBin(InputStream in) throws IOException {
        if (in == null) throw new IOException(new NullPointerException("InputStream is null!"));

        int count;
        byte[] buffer = new byte[4096];
        try (FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "aria2c"))) {
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMPORT_BIN_CODE && resultCode == RESULT_OK && data.getData() != null) {
            try {
                writeStreamAsBin(getContentResolver().openInputStream(data.getData()));
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
        layout.loadListData(new ReleasesAdapter(this, result, this));
    }

    @Override
    public void onException(@NonNull Exception ex) {
        Logging.log(ex);
        layout.showError(R.string.failedRetrievingReleases_reason, ex.getMessage());
    }

    @Override
    public void onReleaseSelected(@NonNull GitHubApi.Release release) {
        layout.showInfo(R.string.downloadingBin);

        downloader.setRelease(release);
        downloader.downloadRelease(this);
    }

    @Override
    public void doneDownload(@NonNull File tmp) {
        layout.showInfo(R.string.extractingBin);
        downloader.extractTo(getEnvDir(), (entry, name) -> name.equals("aria2c"), this);
    }

    @NonNull
    private File getEnvDir() {
        return new File(getFilesDir(), "env");
    }

    @Override
    public void failedDownload(@NonNull Exception ex) {
        onException(ex);
    }

    @Override
    public void doneExtract(@NonNull File dest) {
        layout.showInfo(R.string.binExtracted);

        Prefs.putBoolean(PK.CUSTOM_BIN, false);
        Prefs.putString(PK.ENV_LOCATION, dest.getAbsolutePath());
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    public void failedExtract(@NonNull Exception ex) {
        onException(ex);
    }
}
