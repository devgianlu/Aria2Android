package com.gianlu.aria2android.aria2;

import java.io.InputStream;

public interface IAria2 {
    void onServerStarted(InputStream in, InputStream err);

    void onException(Exception ex, boolean fatal);

    void onServerStopped();
}
