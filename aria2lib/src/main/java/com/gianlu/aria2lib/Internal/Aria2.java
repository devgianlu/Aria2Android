package com.gianlu.aria2lib.Internal;

import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.commonutils.Logging;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

public final class Aria2 {
    private static final Pattern TOP_PATTERN = Pattern.compile("(\\d*?)\\s+(\\d*?)\\s+(\\d*?)%\\s(.)\\s+(\\d*?)\\s+(\\d*?)K\\s+(\\d*?)K\\s+(..)\\s(.*?)\\s+(.*)$");
    private static Aria2 instance;
    private final MessageHandler messageHandler;
    private Env env;
    private Monitor monitor;
    private StreamWatcher errorWatcher;
    private StreamWatcher inputWatcher;
    private Process currentProcess;

    private Aria2() {
        messageHandler = new MessageHandler();
        new Thread(messageHandler).start();
    }

    @NonNull
    public static Aria2 get() {
        if (instance == null) instance = new Aria2();
        return instance;
    }

    @NonNull
    private static String startCommandForLog(@NonNull String exec, String... params) {
        StringBuilder builder = new StringBuilder(exec);
        for (String param : params) builder.append(' ').append(param);
        return builder.toString();
    }

    void addListener(@NonNull MessageListener listener) {
        messageHandler.listeners.add(listener);
    }

    void removeListener(@NonNull MessageListener listener) {
        messageHandler.listeners.remove(listener);
    }

    @NonNull
    public String version() throws BadEnvironmentException, IOException {
        if (env == null)
            throw new BadEnvironmentException("Missing environment!");

        try {
            Process process = execWithParams(false, "-v");
            process.waitFor();
            return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

    @NonNull
    private Process execWithParams(boolean redirect, String... params) throws BadEnvironmentException, IOException {
        if (env == null)
            throw new BadEnvironmentException("Missing environment!");

        String[] cmdline = new String[params.length + 1];
        cmdline[0] = env.execPath();
        System.arraycopy(params, 0, cmdline, 1, params.length);
        return new ProcessBuilder(cmdline).redirectErrorStream(redirect).start();
    }

    public void loadEnv(@NonNull File env) throws BadEnvironmentException {
        if (!env.isDirectory())
            throw new BadEnvironmentException(env.getAbsolutePath() + " is not a directory!");

        File exec = new File(env, "aria2c");
        if (!exec.exists())
            throw new BadEnvironmentException(exec.getAbsolutePath() + " doesn't exists!");

        if (!exec.canExecute() && !exec.setExecutable(true))
            throw new BadEnvironmentException(exec.getAbsolutePath() + " can't be executed!");

        this.env = new Env(env, exec);
    }

    void start() throws BadEnvironmentException, IOException {
        if (env == null)
            throw new BadEnvironmentException("Missing environment!");

        reloadEnv();

        String execPath = env.execPath();
        String[] params = env.startArgs();

        currentProcess = execWithParams(true, params);
        new Thread(new Waiter(currentProcess), "aria2android-waiterThread").start();
        new Thread(this.monitor = new Monitor(), "aria2android-monitorThread").start();
        new Thread(this.inputWatcher = new StreamWatcher(currentProcess.getInputStream()), "aria2-android-inputWatcherThread").start();
        new Thread(this.errorWatcher = new StreamWatcher(currentProcess.getErrorStream()), "aria2-android-errorWatcherThread").start();

        postMessage(Message.obtain(Message.Type.PROCESS_STARTED, startCommandForLog(execPath, params)));
    }

    private void reloadEnv() throws BadEnvironmentException {
        if (env == null)
            throw new BadEnvironmentException("Missing environment!");

        loadEnv(env.baseDir);
    }

    private void processTerminated(int code) {
        postMessage(Message.obtain(Message.Type.PROCESS_TERMINATED, code));

        if (monitor != null) {
            monitor.close();
            monitor = null;
        }

        if (errorWatcher != null) {
            errorWatcher.close();
            errorWatcher = null;
        }

        if (inputWatcher != null) {
            inputWatcher.close();
            inputWatcher = null;
        }
    }

    private void monitorFailed(@NonNull Exception ex) {
        postMessage(Message.obtain(Message.Type.MONITOR_FAILED, ex));
        Logging.log(ex);
    }

    private void monitorGotLine(@NonNull String line) {
        Matcher matcher = TOP_PATTERN.matcher(line);
        if (matcher.find()) {
            postMessage(Message.obtain(Message.Type.MONITOR_UPDATE,
                    MonitorUpdate.obtain(matcher.group(1), matcher.group(3), matcher.group(7))));
        } else {
            Logging.log("Bad `top` line: " + line, true);
        }
    }

    private void postMessage(@NonNull Message message) {
        messageHandler.queue.add(message);
    }

    private void handleStreamMessage(@NonNull String line) {
        if (line.startsWith("WARNING: ")) {
            postMessage(Message.obtain(Message.Type.PROCESS_WARN, line.substring(9)));
        } else if (line.startsWith("ERROR: ")) {
            postMessage(Message.obtain(Message.Type.PROCESS_ERROR, line.substring(7)));
        } else {
            postMessage(Message.obtain(Message.Type.PROCESS_INFO, line));
        }
    }

    void stop() {
        if (currentProcess != null) {
            currentProcess.destroy();
            currentProcess = null;
        }
    }

    public boolean delete() {
        stop();
        return env.delete();
    }

    public interface MessageListener {
        void onMessage(@NonNull Message msg);
    }

    private static class MessageHandler implements Runnable, Closeable {
        private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
        private final List<MessageListener> listeners = new ArrayList<>();
        private volatile boolean shouldStop = false;

        @Override
        public void run() {
            while (!shouldStop) {
                try {
                    Message msg = queue.take();
                    for (MessageListener listener : new ArrayList<>(listeners))
                        listener.onMessage(msg);

                    msg.recycle();
                } catch (InterruptedException ex) {
                    close();
                    Logging.log(ex);
                }
            }
        }

        @Override
        public void close() {
            shouldStop = true;
        }
    }

    private static class Env {
        private final File baseDir;
        private final File exec;
        private final Map<String, String> params;

        Env(@NonNull File baseDir, @NonNull File exec) {
            this.baseDir = baseDir;
            this.exec = exec;
            this.params = new HashMap<>(); // TODO: Load
            params.put("--rpc-listen-all", "true");
            params.put("--enable-color", "false");
            params.put("--enable-rpc", "true");
            params.put("--rpc-secret", "aria2");
            params.put("--rpc-listen-port", "6800");
        }

        @NonNull
        String[] startArgs() {
            String[] args = new String[params.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty())
                    args[i] = entry.getKey();
                else
                    args[i] = entry.getKey() + "=" + entry.getValue();

                i++;
            }
            return args;
        }

        @NonNull
        String execPath() {
            return exec.getAbsolutePath();
        }

        boolean delete() {
            boolean fine = true;
            for (File file : baseDir.listFiles())
                if (!file.delete()) fine = false;
            return fine;
        }
    }

    private class StreamWatcher implements Runnable, Closeable {
        private final InputStream stream;
        private volatile boolean shouldStop = false;

        StreamWatcher(@NonNull InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            try (Scanner scanner = new Scanner(stream)) {

                while (!shouldStop && scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (!line.isEmpty()) handleStreamMessage(line);
                }
            }
        }

        @Override
        public void close() {
            shouldStop = true;
        }
    }

    private class Monitor implements Runnable, Closeable {
        private volatile boolean shouldStop = false;

        @Override
        public void run() {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("top -d 1");
                try (Scanner scanner = new Scanner(process.getInputStream())) {
                    while (!shouldStop && scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (line.endsWith("aria2c"))
                            monitorGotLine(line);
                    }
                }
            } catch (IOException ex) {
                monitorFailed(ex);
            } finally {
                if (process != null) process.destroy();
            }
        }

        @Override
        public void close() {
            shouldStop = true;
        }
    }

    private class Waiter implements Runnable {
        private final Process process;

        Waiter(@NonNull Process process) {
            this.process = process;
        }

        @Override
        public void run() {
            try {
                int exit = process.waitFor();
                processTerminated(exit);
            } catch (InterruptedException ex) {
                processTerminated(999);
                Logging.log(ex);
            }
        }
    }
}
