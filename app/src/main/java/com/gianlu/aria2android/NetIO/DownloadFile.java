package com.gianlu.aria2android.NetIO;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.gianlu.aria2android.R;
import com.gianlu.aria2android.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFile extends AsyncTask<URL, Integer, Object> {
    private Activity context;
    private ProgressDialog progressDialog;

    public DownloadFile(Activity context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(false);
                progressDialog.setMessage(context.getString(R.string.downloading_bin));

                progressDialog.show();
            }
        });
    }

    @Override
    protected void onPostExecute(Object result) {
        progressDialog.dismiss();

        if (result instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream out = (ByteArrayOutputStream) result;

            try {
                Utils.unzipBin(new ByteArrayInputStream(out.toByteArray()), context);
            } catch (IOException ex) {
                Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_DOWNLOADING_BIN, ex);
            }
        } else if (result instanceof String) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_DOWNLOADING_BIN, (String) result);
        } else if (result instanceof Exception) {
            Utils.UIToast(context, Utils.TOAST_MESSAGES.FAILED_DOWNLOADING_BIN, (Exception) result);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        progressDialog.setProgress(values[0]);
    }

    @Override
    protected Object doInBackground(URL... params) {
        try {
            HttpURLConnection conn = (HttpURLConnection) params[0].openConnection();
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "#" + conn.getResponseCode() + ": " + conn.getResponseMessage();
            }
            int fileLength = conn.getContentLength();
            progressDialog.setMax(fileLength);

            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = in.read(data)) != -1) {
                if (isCancelled()) {
                    in.close();
                    return null;
                }

                total += count;
                if (fileLength > 0) publishProgress((int) (total * 100 / fileLength));
                out.write(data, 0, count);
            }

            return out;
        } catch (Exception ex) {
            return ex;
        }
    }
}
