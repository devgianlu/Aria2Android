package com.gianlu.aria2android.Logging;

public class LoglineItem {
    private TYPE type;
    private String message;

    public LoglineItem(TYPE type, String message) {
        this.type = type;
        this.message = message;
    }

    public TYPE getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public enum TYPE {
        INFO,
        WARNING,
        ERROR
    }
}
