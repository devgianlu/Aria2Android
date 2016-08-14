package com.gianlu.aria2android.NetIO;

public interface IResponse {
    void onStart();

    void onResponse(String response);

    void onException(Exception exception);

    void onFailed(int code, String message);
}
