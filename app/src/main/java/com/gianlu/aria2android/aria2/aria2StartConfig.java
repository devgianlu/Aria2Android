package com.gianlu.aria2android.aria2;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

public class aria2StartConfig implements Parcelable {
    public static final Creator<aria2StartConfig> CREATOR = new Creator<aria2StartConfig>() {
        @Override
        public aria2StartConfig createFromParcel(Parcel in) {
            return new aria2StartConfig(in);
        }

        @Override
        public aria2StartConfig[] newArray(int size) {
            return new aria2StartConfig[size];
        }
    };
    private final String outputDirectory;
    private Map<String, String> options;
    private final boolean useConfig;
    private final boolean saveSession;
    private final int rpcPort;
    private final String rpcToken;

    public aria2StartConfig(String outputDirectory, Map<String, String> options, boolean useConfig, boolean saveSession, int rpcPort, String rpcToken) {
        this.outputDirectory = outputDirectory;
        this.options = options;
        this.useConfig = useConfig;
        this.saveSession = saveSession;
        this.rpcPort = rpcPort;
        this.rpcToken = rpcToken;
    }

    private aria2StartConfig(Parcel in) {
        outputDirectory = in.readString();
        useConfig = in.readByte() != 0;
        saveSession = in.readByte() != 0;
        rpcPort = in.readInt();
        rpcToken = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(outputDirectory);
        dest.writeByte((byte) (useConfig ? 1 : 0));
        dest.writeByte((byte) (saveSession ? 1 : 0));
        dest.writeInt(rpcPort);
        dest.writeString(rpcToken);
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public boolean useConfig() {
        return useConfig;
    }

    public int getRpcPort() {
        return rpcPort;
    }

    public String getRpcToken() {
        return rpcToken;
    }

    public boolean isSavingSession() {
        return saveSession;
    }
}
