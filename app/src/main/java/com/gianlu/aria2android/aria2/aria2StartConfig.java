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
    private String outputDirectory;
    private Map<String, String> options;
    private boolean useConfig;
    private int rpcPort;
    private String rpcToken;

    public aria2StartConfig(String outputDirectory, Map<String, String> options, boolean useConfig, int rpcPort, String rpcToken) {
        this.outputDirectory = outputDirectory;
        this.options = options;
        this.useConfig = useConfig;
        this.rpcPort = rpcPort;
        this.rpcToken = rpcToken;
    }

    protected aria2StartConfig(Parcel in) {
        outputDirectory = in.readString();
        useConfig = in.readByte() != 0;
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
}
