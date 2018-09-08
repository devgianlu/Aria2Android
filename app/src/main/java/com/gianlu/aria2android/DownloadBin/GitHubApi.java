package com.gianlu.aria2android.DownloadBin;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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

public class GitHubApi {
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
    private static SimpleDateFormat getDateParser() {
        return new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.getDefault());
    }

    @WorkerThread
    private HttpURLConnection basicRequestSync(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.connect();

        if (conn.getResponseCode() == 200) return conn;
        else
            throw new IOException(String.format("%d: %s", conn.getResponseCode(), conn.getResponseMessage()));
    }

    public void getReleases(final String author, final String repo, final OnResult<List<Release>> listener) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = basicRequestSync("https://api.github.com/repos/" + author + "/" + repo + "/releases");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder builder = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null)
                        builder.append(line);

                    JSONArray array = new JSONArray(builder.toString());
                    final List<Release> releases = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++)
                        releases.add(new Release(array.getJSONObject(i)));

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onResult(releases);
                        }
                    });
                } catch (IOException | JSONException | ParseException ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onException(ex);
                        }
                    });
                }
            }
        });
    }

    public interface OnResult<E> {
        @UiThread
        void onResult(@NonNull E result);

        @UiThread
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
