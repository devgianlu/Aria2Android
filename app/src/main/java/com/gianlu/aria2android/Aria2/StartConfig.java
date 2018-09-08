package com.gianlu.aria2android.Aria2;

import android.os.Environment;
import android.support.annotation.NonNull;

import com.gianlu.aria2android.PK;
import com.gianlu.aria2android.Utils;
import com.gianlu.commonutils.Preferences.Prefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;

public class StartConfig implements Serializable {
    public final String outputDirectory;
    public final HashMap<String, String> options;
    public final boolean saveSession;
    public final int rpcPort;
    public final String rpcToken;
    public final boolean allowOriginAll;

    public StartConfig(String outputDirectory, HashMap<String, String> options, boolean saveSession, int rpcPort, String rpcToken, boolean allowOriginAll) {
        this.outputDirectory = outputDirectory;
        this.options = options;
        this.saveSession = saveSession;
        this.rpcPort = rpcPort;
        this.rpcToken = rpcToken;
        this.allowOriginAll = allowOriginAll;
    }

    @NonNull
    public static StartConfig fromPrefs() throws JSONException {
        HashMap<String, String> options = new HashMap<>();
        Utils.toMap(new JSONObject(Prefs.getBase64String(PK.CUSTOM_OPTIONS, "{}")), options);
        return new StartConfig(Prefs.getString(PK.OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                options,
                Prefs.getBoolean(PK.SAVE_SESSION),
                Prefs.getInt(PK.RPC_PORT),
                Prefs.getString(PK.RPC_TOKEN),
                Prefs.getBoolean(PK.RPC_ALLOW_ORIGIN_ALL));
    }
}
