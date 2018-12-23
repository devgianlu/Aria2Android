package com.gianlu.aria2lib;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

public final class GitHubApi {
    private static GitHubApi instance;
    private final Handler handler;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private GitHubApi() {
        this.handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static GitHubApi get() {
        if (instance == null) instance = new GitHubApi();
        return instance;
    }

    @NonNull
    private static String read(@NonNull InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null)
            builder.append(line);

        return builder.toString();
    }

    @NonNull
    private static SimpleDateFormat getDateParser() {
        return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.getDefault());
    }

    void execute(@NonNull Runnable runnable) {
        executorService.execute(runnable);
    }

    void inputStream(@NonNull String url, @NonNull InputStreamWorker worker) {
        executorService.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = basicRequestSync(url);
                worker.work(conn.getInputStream());
            } catch (Exception ex) {
                worker.exception(ex);
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    @WorkerThread
    private HttpURLConnection basicRequestSync(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.connect();

        if (conn.getResponseCode() == 200)
            return conn;
        else
            throw new IOException(String.format("%d: %s", conn.getResponseCode(), conn.getResponseMessage()));
    }

    public void getReleases(@NonNull String owner, @NonNull String repo, @NonNull OnResult<List<Release>> listener) {
        inputStream("https://api.github.com/repos/" + owner + "/" + repo + "/releases", new InputStreamWorker() {
            @Override
            public void work(@NonNull InputStream in) throws IOException, JSONException, ParseException {
                JSONArray array = new JSONArray(read(in));
                final List<Release> releases = new ArrayList<>();
                for (int i = 0; i < array.length(); i++)
                    releases.add(new Release(array.getJSONObject(i)));

                handler.post(() -> listener.onResult(releases));
            }

            @Override
            public void exception(@NonNull Exception ex) {
                handler.post(() -> listener.onException(ex));
            }
        });
    }

    public void getRelease(@NonNull String owner, @NonNull String repo, int id, OnResult<Release> listener) {
        inputStream("https://api.github.com/repos/" + owner + "/" + repo + "/releases/" + id, new InputStreamWorker() {
            @Override
            public void work(@NonNull InputStream in) throws Exception {
                Release release = new Release(new JSONObject(read(in)));
                handler.post(() -> listener.onResult(release));
            }

            @Override
            public void exception(@NonNull Exception ex) {
                handler.post(() -> listener.onException(ex));
            }
        });
    }

    @WorkerThread
    public interface InputStreamWorker {
        void work(@NonNull InputStream in) throws Exception;

        void exception(@NonNull Exception ex);
    }

    @UiThread
    public interface OnResult<E> {
        void onResult(@NonNull E result);

        void onException(@NonNull Exception ex);
    }

    public static class Release {
        public final int id;
        public final String name;
        public final String htmlUrl;
        public final long publishedAt;
        public Asset androidAsset;

        Release(JSONObject obj) throws JSONException, ParseException {
            id = obj.getInt("id");
            name = obj.getString("name");
            htmlUrl = obj.getString("html_url");
            publishedAt = getDateParser().parse(obj.getString("published_at")).getTime();

            JSONArray assets = obj.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (asset.optString("name", "").contains("android")) {
                    androidAsset = new Asset(asset);
                    break;
                }
            }
        }

        public static class Asset {
            public final int id;
            public final String name;
            public final String downloadUrl;
            public final long size;

            Asset(JSONObject obj) throws JSONException {
                id = obj.getInt("id");
                name = obj.getString("name");
                downloadUrl = obj.getString("browser_download_url");
                size = obj.getLong("size");
            }
        }
    }
}
