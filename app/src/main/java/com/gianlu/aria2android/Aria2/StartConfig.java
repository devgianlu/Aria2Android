package com.gianlu.aria2android.Aria2;

import android.content.Context;
import android.os.Environment;

import com.gianlu.aria2android.PKeys;
import com.gianlu.commonutils.Prefs;

import java.io.Serializable;
import java.util.HashMap;

public class StartConfig implements Serializable {
    public final String outputDirectory;
    public final HashMap<String, String> options;
    public final boolean saveSession;
    public final int rpcPort;
    public final String rpcToken;

    public StartConfig(String outputDirectory, HashMap<String, String> options, boolean saveSession, int rpcPort, String rpcToken) {
        this.outputDirectory = outputDirectory;
        this.options = options;
        this.saveSession = saveSession;
        this.rpcPort = rpcPort;
        this.rpcToken = rpcToken;
    }

    public static StartConfig fromPrefs(Context context) {
        return new StartConfig(Prefs.getString(context, PKeys.OUTPUT_DIRECTORY, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()),
                null, // TODO: Config file editor
                Prefs.getBoolean(context, PKeys.SAVE_SESSION, true),
                Prefs.getInt(context, PKeys.RPC_PORT, 6800),
                Prefs.getString(context, PKeys.RPC_TOKEN, "aria2"));
    }
}
