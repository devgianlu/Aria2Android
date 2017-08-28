package com.gianlu.aria2android.Aria2;

import android.content.Context;
import android.os.Environment;

import com.gianlu.aria2android.PKeys;
import com.gianlu.aria2android.Utils;
import com.gianlu.commonutils.Prefs;

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

    public static StartConfig fromPrefs(Context context) throws JSONException {
        HashMap<String, String> options = new HashMap<>();
        Utils.toMap(new JSONObject(Prefs.getBase64String(context, PKeys.CUSTOM_OPTIONS, "{}")), options);
        return new StartConfig(Prefs.getString(context, PKeys.OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                options,
                Prefs.getBoolean(context, PKeys.SAVE_SESSION, true),
                Prefs.getInt(context, PKeys.RPC_PORT, 6800),
                Prefs.getString(context, PKeys.RPC_TOKEN, "aria2"),
                Prefs.getBoolean(context, PKeys.RPC_ALLOW_ORIGIN_ALL, false));
    }
}
