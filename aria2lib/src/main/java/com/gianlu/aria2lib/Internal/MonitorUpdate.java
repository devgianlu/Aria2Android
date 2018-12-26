package com.gianlu.aria2lib.Internal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

import androidx.annotation.NonNull;

public final class MonitorUpdate implements Serializable {
    private static final Queue<MonitorUpdate> cache = new LinkedList<>();

    static {
        for (int i = 0; i < 10; i++)
            cache.add(new MonitorUpdate());
    }

    private boolean recycled = false;
    private String rss;
    private String cpu;
    private String pid;

    private MonitorUpdate() {
    }

    @NonNull
    public static MonitorUpdate obtain(@NonNull String pid, @NonNull String cpu, @NonNull String rss) {
        MonitorUpdate msg = cache.poll();
        if (msg == null) msg = new MonitorUpdate();
        msg.recycled = false;
        msg.pid = pid;
        msg.cpu = cpu;
        msg.rss = rss;
        return msg;
    }

    public void recycle() {
        if (!recycled) {
            cache.add(this);
            recycled = true;
        }
    }

    @NonNull
    public String pid() {
        return pid;
    }

    @NonNull
    public String cpu() {
        return cpu;
    }

    @NonNull
    public String rss() {
        return rss;
    }
}
