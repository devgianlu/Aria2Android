package com.gianlu.aria2android;

import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import com.gianlu.aria2android.databinding.AriangLayoutBinding;
import com.gianlu.aria2lib.Aria2PK;

import java.io.IOException;
import java.util.Objects;

public class AriaNgActivity extends BastActivity {
    WebView webView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AriangLayoutBinding binding = AriangLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        webView = binding.ariangWebview;
        setupWebView();
    }

    void setupWebView() {
        WebSettings settings = webView.getSettings();
        webView.setWebViewClient(new WebViewClient());
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.loadUrl(getUri());
    }

    private String getUri() {
        String secret = Base64.encodeToString(Aria2PK.RPC_TOKEN.fallback().getBytes(), Base64.DEFAULT);
        return "http://ariang.local/index.html" +
                "#!/settings/rpc/set?" +
                "protocol=ws&" +
                "host=127.0.0.1&" +
                "port=" + Aria2PK.RPC_PORT.fallback() + "&" +
                "interface=jsonrpc&" +
                "secret=" + secret;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) webView.destroy();
    }

    static class WebViewClient extends android.webkit.WebViewClient {
        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (Objects.equals(request.getUrl().getPath(), "/index.html")) {
                try {
                    return new WebResourceResponse("text/html", "utf-8",
                            view.getContext().getAssets().open("ariang/index.html"));
                } catch (IOException e) {
                    return null;
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }
}
