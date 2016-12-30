package com.gianlu.aria2android.aria2;

import java.io.Serializable;
import java.util.Map;

public class aria2StartConfig implements Serializable {
    public final String outputDirectory;
    public final Map<String, String> options;
    public final boolean useConfig;
    public final String configFile;
    public final boolean saveSession;
    public final int rpcPort;
    public final String rpcToken;

    public aria2StartConfig(String outputDirectory, Map<String, String> options, boolean useConfig, String configFile, boolean saveSession, int rpcPort, String rpcToken) {
        this.outputDirectory = outputDirectory;
        this.options = options;
        this.useConfig = useConfig;
        this.configFile = configFile;
        this.saveSession = saveSession;
        this.rpcPort = rpcPort;
        this.rpcToken = rpcToken;
    }
}
