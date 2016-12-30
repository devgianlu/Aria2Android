package com.gianlu.aria2android.Logging;

public class LoglineItem {
    private final TYPE type;
    private final String message;

    LoglineItem(TYPE type, String message) {
        this.type = type;
        this.message = message;
    }

    TYPE getType() {
        return type;
    }

    String getMessage() {
        return message;
    }

    public enum TYPE {
        INFO,
        WARNING,
        ERROR
    }
}
