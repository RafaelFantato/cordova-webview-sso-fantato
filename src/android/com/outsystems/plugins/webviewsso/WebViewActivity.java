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
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("WebViewActivity", "onCreate called with intent: " + getIntent().getData());

        webView = new WebView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0, // altura zero, controlada pelo weight
            1  // peso 1 para ocupar todo o espaço disponível
        );
        webView.setLayoutParams(webViewParams);


        // Botão ocupa apenas o necessário
        Button closeButton = new Button(this);
        closeButton.setText("Cancel");
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
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webView.setWebChromeClient(new WebChromeClient());

        String defaultUA = webView.getSettings().getUserAgentString();
        String customUA = defaultUA + " OutSystemsApp v.0.1.0";
        webView.getSettings().setUserAgentString(customUA);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        }
        cookieManager.setAcceptFileSchemeCookies(true);


        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request.getUrl().toString();
                if (!targetUrl.startsWith("http")) {

                    WebViewPlugin.sendEvent("onDeeplinkCalled", targetUrl);
                    finish();
                    return true;
                    /*try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        return true;
                    } catch (Exception e) {
                        Log.e("WebView", "Failed to open deeplink: " + e.getMessage());
                    }*/
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebViewPlugin.sendEvent("onWebViewClosed", ""); // ou envie dados, se quiser
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // garante que getIntent() esteja atualizado


        Uri data = intent.getData();
        Log.d("DEEPLINK", "Received deep link: " + data.toString());

        if (data != null && webView != null) {
            WebViewPlugin.sendEvent("onDeeplinkCalled", "data.toString()");
            // Se quiser encerrar a WebView após o evento:
            runOnUiThread(() -> finish());
            //webView.loadUrl(data.toString());
        }
    }

    private String extractUrlFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            return data.toString(); // Para deep links tipo myapp://...
        }
        return intent.getStringExtra("url");
    }
}