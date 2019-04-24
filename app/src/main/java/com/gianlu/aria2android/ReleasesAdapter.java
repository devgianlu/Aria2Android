package com.gianlu.aria2android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.aria2lib.GitHubApi;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.SuperTextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReleasesAdapter extends RecyclerView.Adapter<ReleasesAdapter.ViewHolder> {
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GitHubApi.Release release = releases.get(position);
        holder.name.setText(release.name);
        holder.uploadedAt.setHtml(R.string.publishedAt, CommonUtils.getFullDateFormatter().format(new Date(release.publishedAt)));
        holder.size.setHtml(R.string.size, CommonUtils.dimensionFormatter(release.androidAsset.size, false));
        holder.itemView.setOnClickListener(view1 -> {
            if (listener != null) listener.onReleaseSelected(release);
        });
    }

    @Override
    public int getItemCount() {
        return releases.size();
    }

    public interface Listener {
        void onReleaseSelected(@NonNull GitHubApi.Release release);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final SuperTextView uploadedAt;
        final SuperTextView size;

        ViewHolder(@NonNull ViewGroup parent) {
            super(inflater.inflate(R.layout.release_item, parent, false));

            name = itemView.findViewById(R.id.releaseItem_name);
            uploadedAt = itemView.findViewById(R.id.releaseItem_publishedAt);
            size = itemView.findViewById(R.id.releaseItem_size);
        }
    }
}
