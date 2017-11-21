package com.gianlu.aria2android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gianlu.aria2android.DownloadBin.ReleasesAdapter;
import com.gianlu.aria2android.NetIO.GitHubApi;
import com.gianlu.commonutils.MessageLayout;

import java.util.List;

public class DownloadBinActivity extends AppCompatActivity implements GitHubApi.IResult<List<GitHubApi.Release>>, ReleasesAdapter.IAdapter, BinUtils.IDownloadAndExtractBin {
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
