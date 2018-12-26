package com.gianlu.aria2lib;

import androidx.annotation.NonNull;

public class BadEnvironmentException extends Exception {
    public BadEnvironmentException(String message) {
        super(message);
    }

    public BadEnvironmentException(@NonNull Throwable ex) {
        super(ex);
    }
}
