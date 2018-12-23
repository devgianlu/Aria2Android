package com.gianlu.aria2lib;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * Workflow:
 * - {@link #pickRelease(PickRelease)}
 * - {@link #downloadRelease(DownloadRelease)}
 * - {@link #extractTo(File, Filter, ExtractTo)}
 */
public final class Aria2Downloader {
    private final GitHubApi gitHub;
    private final Handler handler;
    private GitHubApi.Release selectedRelease;
    private File downloadTmpFile;

    public Aria2Downloader() {
        gitHub = GitHubApi.get();
        handler = new Handler(Looper.getMainLooper());
    }

    public void extractTo(@NonNull File dest, @Nullable Filter filter, @NonNull ExtractTo listener) {
        if (downloadTmpFile == null) {
            listener.failedExtract(new IllegalStateException("Missing downloaded release!"));
            return;
        }

        if (dest.exists()) {
            if (!dest.isDirectory()) {
                listener.failedExtract(new IllegalStateException(dest.getAbsolutePath() + " is not a directory!"));
                return;
            }
        } else {
            if (!dest.mkdir()) {
                listener.failedExtract(new IllegalStateException("Failed creating " + dest.getAbsolutePath()));
                return;
            }
        }

        gitHub.execute(() -> {
            byte[] buffer = new byte[4096];
            int read;

            try (ZipInputStream in = new ZipInputStream(new FileInputStream(downloadTmpFile))) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if (entry.isDirectory())
                        continue;

                    String[] split = entry.getName().split(Pattern.quote(File.separator));
                    String name = split.length == 1 ? split[0] : split[split.length - 1];

                    if (filter != null && !filter.accept(entry, name))
                        continue;

                    try (FileOutputStream out = new FileOutputStream(new File(dest, name))) {
                        while ((read = in.read(buffer)) != -1)
                            out.write(buffer, 0, read);
                    }

                    in.closeEntry();
                }

                handler.post(() -> listener.doneExtract(dest));
            } catch (IOException ex) {
                handler.post(() -> listener.failedExtract(ex));
            }
        });
    }

    public void downloadRelease(@NonNull DownloadRelease listener) {
        if (selectedRelease == null) {
            listener.failedDownload(new IllegalStateException("Did not select a release!"));
            return;
        }

        if (selectedRelease.androidAsset == null) {
            listener.failedDownload(new IllegalStateException("This release hasn't an Android asset!"));
            return;
        }

        gitHub.inputStream(selectedRelease.androidAsset.downloadUrl, new GitHubApi.InputStreamWorker() {
            @Override
            public void work(@NonNull InputStream in) throws Exception {
                File tmp = File.createTempFile("aria2", String.valueOf(selectedRelease.androidAsset.id));
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1)
                        out.write(buffer, 0, read);
                }

                setDownloadTmpFile(tmp);
                handler.post(() -> listener.doneDownload(tmp));
            }

            @Override
            public void exception(@NonNull Exception ex) {
                handler.post(() -> listener.failedDownload(ex));
            }
        });
    }

    public void setDownloadTmpFile(@NonNull File file) {
        this.downloadTmpFile = file;
    }

    public void pickRelease(@NonNull PickRelease listener) {
        gitHub.getReleases("aria2", "aria2", new GitHubApi.OnResult<List<GitHubApi.Release>>() {
            @Override
            public void onResult(@NonNull List<GitHubApi.Release> result) {
                setRelease(listener.pick(result));
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.failedRetrieveRelease(ex);
            }
        });
    }

    public void getReleases(@NonNull GitHubApi.OnResult<List<GitHubApi.Release>> listener) {
        gitHub.getReleases("aria2", "aria2", listener);
    }

    public void setRelease(@NonNull GitHubApi.Release release) {
        this.selectedRelease = release;
    }

    public void setReleaseId(int id, @NonNull SetRelease listener) {
        gitHub.getRelease("aria2", "aria2", id, new GitHubApi.OnResult<GitHubApi.Release>() {
            @Override
            public void onResult(@NonNull GitHubApi.Release result) {
                setRelease(result);
                listener.doneRetrieveRelease(result);
            }

            @Override
            public void onException(@NonNull Exception ex) {
                listener.failedRetrieveRelease(ex);
            }
        });
    }

    public interface Filter {
        boolean accept(@NonNull ZipEntry entry, @NonNull String name);
    }

    @UiThread
    public interface ExtractTo {
        void doneExtract(@NonNull File dest);

        void failedExtract(@NonNull Exception ex);
    }

    @UiThread
    public interface DownloadRelease {
        void doneDownload(@NonNull File tmp);

        void failedDownload(@NonNull Exception ex);
    }

    @UiThread
    public interface SetRelease {
        void doneRetrieveRelease(@NonNull GitHubApi.Release release);

        void failedRetrieveRelease(@NonNull Exception ex);
    }

    @UiThread
    public interface PickRelease {

        @NonNull
        GitHubApi.Release pick(@NonNull List<GitHubApi.Release> result);

        void failedRetrieveRelease(@NonNull Exception ex);
    }
}
