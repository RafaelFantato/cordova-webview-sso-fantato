package com.outsystems.plugins.webviewsso;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.LinearLayout;
import android.widget.Button;

public class WebViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request.getUrl().toString();
                if (!targetUrl.startsWith("http")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    } catch (Exception e) {
                        Log.e("WebView", "Failed to open deeplink: " + e.getMessage());
                    }
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                WebViewPlugin.sendEvent("loadstart", url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                WebViewPlugin.sendEvent("loadstop", url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                WebViewPlugin.sendEvent("loaderror", error.getDescription().toString());
            }
        });

        Button closeButton = new Button(this);
        closeButton.setText("Close");
        closeButton.setOnClickListener(v -> finish());

        layout.addView(webView);
        layout.addView(closeButton);
        setContentView(layout);

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            webView.loadUrl(url);
        }
    }
}