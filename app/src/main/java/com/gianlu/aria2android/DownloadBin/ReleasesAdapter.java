package com.gianlu.aria2android.DownloadBin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gianlu.aria2android.R;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;

public class ReleasesAdapter extends BaseAdapter {
    private final List<GitHubApi.Release> releases;
    private final LayoutInflater inflater;
    private final Listener listener;

    public ReleasesAdapter(@NonNull Context context, List<GitHubApi.Release> releases, Listener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.releases = new ArrayList<>();
        for (GitHubApi.Release release : releases)
            if (release.androidAsset != null)
                this.releases.add(release);
    }

    @Override
    public int getCount() {
        return releases.size();
    }

    @Override
    public GitHubApi.Release getItem(int i) {
        return releases.get(i);
    }

    @Override
    public long getItemId(int i) {
        return getItem(i).id;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) view = inflater.inflate(R.layout.release_item, viewGroup, false);
        final GitHubApi.Release release = getItem(i);
        TextView name = view.findViewById(R.id.releaseItem_name);
        name.setText(release.name);
        SuperTextView uploadedAt = view.findViewById(R.id.releaseItem_publishedAt);
        uploadedAt.setHtml(R.string.publishedAt, CommonUtils.getFullDateFormatter().format(new Date(release.publishedAt)));
        SuperTextView size = view.findViewById(R.id.releaseItem_size);
        size.setHtml(R.string.size, CommonUtils.dimensionFormatter(release.androidAsset.size, false));
        view.setOnClickListener(view1 -> {
            if (listener != null) listener.onReleaseSelected(release);
        });
        return view;
    }

    public interface Listener {
        void onReleaseSelected(@NonNull GitHubApi.Release release);
    }
}
