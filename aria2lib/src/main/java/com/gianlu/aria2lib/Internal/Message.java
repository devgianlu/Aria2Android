package com.gianlu.aria2lib.Internal;

import java.util.LinkedList;
import java.util.Queue;

import androidx.annotation.NonNull;

public final class Message {
    private static final Queue<Message> cache = new LinkedList<>();

    static {
        for (int i = 0; i < 10; i++)
            cache.add(new Message());
    }

    public Object o;
    public int i;
    public Type type;
    private boolean recycled = false;

    private Message() {
    }

    @NonNull
    public static Message obtain(@NonNull Type type, int i, Object o) {
        Message msg = cache.poll();
        if (msg == null) msg = new Message();
        msg.recycled = false;
        msg.type = type;
        msg.i = i;
        msg.o = o;
        return msg;
    }

    @NonNull
    public static Message obtain(@NonNull Type type, Object o) {
        return obtain(type, 0, o);
    }

    @NonNull
    public static Message obtain(@NonNull Type type, int i) {
        return obtain(type, i, null);
    }

    @NonNull
    public static Message obtain(@NonNull Type type) {
        return obtain(type, 0, null);
    }

    public void recycle() {
        if (!recycled) {
            cache.add(this);
            recycled = true;
        }
    }

    public enum Type {
        PROCESS_TERMINATED, PROCESS_STARTED, MONITOR_FAILED, MONITOR_UPDATE,
        PROCESS_WARN, PROCESS_ERROR, PROCESS_INFO
    }
}
