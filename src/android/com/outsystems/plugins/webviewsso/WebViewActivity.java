package com.outsystems.plugins.webviewsso;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

// Estes 3 abaixo são essenciais para os erros que você reportou:
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.CookieManager;

import android.widget.LinearLayout;
import android.widget.Button;

import androidx.annotation.RequiresApi;

public class WebViewActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        WebView webView = new WebView(this);

        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, // altura zero, controlada pelo weight
            1  // peso 1 para ocupar todo o espaço disponível
        );
        webView.setLayoutParams(webViewParams);


        // Botão ocupa apenas o necessário
        Button closeButton = new Button(this);
        closeButton.setText("Close");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeButton.setLayoutParams(buttonParams);

        // Botão fecha a activity
        closeButton.setOnClickListener(v -> finish());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.setWebChromeClient(new WebChromeClient());

        String defaultUA = webView.getSettings().getUserAgentString();
        String customUA = defaultUA + " mobileapptest/1.0";
        webView.getSettings().setUserAgentString(customUA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        }


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



        layout.addView(webView);
        layout.addView(closeButton);
        setContentView(layout);

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            webView.loadUrl(url);
        }
    }
}