package com.gianlu.aria2android.Aria2;

import com.gianlu.commonutils.Logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamListener extends Thread {
    private final InputStream in;
    private final InputStream err;
    private final IStreamListener listener;
    private volatile boolean _shouldStop;

    public StreamListener(InputStream in, InputStream err, IStreamListener listener) {
        this.in = in;
        this.err = err;
        this.listener = listener;
    }

    public void stopSafe() {
        _shouldStop = true;
    }

    @Override
    public void run() {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.in));
        BufferedReader err = new BufferedReader(new InputStreamReader(this.err));

        while (!_shouldStop) {
            try {
                String inLine;
                if ((inLine = in.readLine()) != null) {
                    listener.onNewLogLine(new Logging.LogLine(Logging.LogLine.Type.INFO, inLine));
                }

                String errLine;
                if ((errLine = err.readLine()) != null) {
                    if (errLine.startsWith("WARNING:")) {
                        listener.onNewLogLine(new Logging.LogLine(Logging.LogLine.Type.WARNING, errLine.replace("WARNING:", "")));
                    } else if (errLine.startsWith("ERROR:")) {
                        listener.onNewLogLine(new Logging.LogLine(Logging.LogLine.Type.ERROR, errLine.replace("ERROR:", "")));
                    }
                }
            } catch (IOException ex) {
                listener.onNewLogLine(new Logging.LogLine(Logging.LogLine.Type.ERROR, "Failed parsing the log line: " + ex.getMessage()));
            }
        }
    }

    public interface IStreamListener {
        void onNewLogLine(Logging.LogLine line);
    }
}