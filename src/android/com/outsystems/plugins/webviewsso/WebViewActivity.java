package com.outsystems.plugins.webviewsso;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.security.KeyChain;
import android.util.Log;
import android.view.View;
import android.webkit.ClientCertRequest;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class WebViewActivity extends Activity {
    private WebView webView;
    private ProgressBar loadingSpinner;
    private String appUUID;
    // Adicione uma variável de controle na classe WebViewActivity
    private boolean deeplinkHandled = false;
    // Defina uma TAG constante para facilitar a filtragem no Logcat
    private static final String TAG = "WebViewActivitySSO";

    private String clientCertAlias;
    private boolean clientCertEnabledByTrigger;
    private volatile boolean clientCertArmed;
    private final Set<String> clientCertAllowedHosts = new HashSet<>();

    private class NativeBridge {
        @JavascriptInterface
        public void armClientCertificate() {
            clientCertArmed = true;
            Log.d(TAG, "Client certificate armed by page trigger");
            WebViewPlugin.sendEvent("clientcert_armed", "");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called with intent: " + getIntent().getData());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Criar FrameLayout para envolver WebView + ProgressBar
        FrameLayout webViewContainer = new FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1
        );
        webViewContainer.setLayoutParams(containerParams);

        webView = new WebView(this);
        FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        webView.setLayoutParams(webViewParams);

        // Criar spinner de loading
        loadingSpinner = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.CENTER
        );
        loadingSpinner.setLayoutParams(spinnerParams);

        webViewContainer.addView(webView);
        webViewContainer.addView(loadingSpinner);


        // Botão ocupa apenas o necessário
        Button closeButton = new Button(this);
        String buttonText = getIntent().getStringExtra("buttonText");
        if (buttonText == null || buttonText.isEmpty()) {
            buttonText = "Abbrechen";  // Valor padrão
        }
        closeButton.setText(buttonText);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeButton.setLayoutParams(buttonParams);

        // Ajustes visuais: fundo branco para o botão e texto escuro
        closeButton.setBackgroundColor(Color.parseColor("#FFFFFF"));
        closeButton.setTextColor(Color.parseColor("#707070"));
        closeButton.setAllCaps(false);

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

        String tempVersion = getIntent().getStringExtra("PlatformVersion");
        if (tempVersion == null || tempVersion.isEmpty()) {
            tempVersion = "ODC";  // Valor padrão
        }
        final String platformVersion = tempVersion;

        clientCertAlias = getIntent().getStringExtra("clientCertAlias");
        clientCertEnabledByTrigger = getIntent().getBooleanExtra("clientCertEnabledByTrigger", true);
        clientCertArmed = !clientCertEnabledByTrigger;

        ArrayList<String> allowedHosts = getIntent().getStringArrayListExtra("clientCertAllowedHosts");
        if (allowedHosts != null) {
            for (String host : allowedHosts) {
                if (host != null && !host.trim().isEmpty()) {
                    clientCertAllowedHosts.add(host.trim().toLowerCase(Locale.US));
                }
            }
        }

        webView.addJavascriptInterface(new NativeBridge(), "WebViewPluginNative");

        webView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request.getUrl().toString();
                Log.d(TAG, targetUrl);
                
                if (!targetUrl.startsWith("http")) {
                    
                    Log.d(TAG, "Returning Deeplink to Plugin: " + targetUrl);
                    
                    // 1. Define a flag como true
                    deeplinkHandled = true;
                    
                    if ("ODC".equals(platformVersion)) {
                        // Substitua a chamada estática por este bloco:
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("deeplink_result", targetUrl);
                        setResult(Activity.RESULT_OK, resultIntent); // Define o resultado como OK
                        
                        finish(); // Encerra a Activity e retorna o resultado
                        return true;
                    }else{

                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            return true;
                        } catch (Exception e) {
                            Log.e("WebView", "Failed to open deeplink: " + e.getMessage());
                        }
                    }

                            
                    
                }
                return false;
            }

            @Override
            public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
                handleClientCertRequest(request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadingSpinner.setVisibility(View.VISIBLE);
                WebViewPlugin.sendEvent("loadstart", url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadingSpinner.setVisibility(View.GONE);
                WebViewPlugin.sendEvent("loadstop", url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                WebViewPlugin.sendEvent("loaderror", error.getDescription().toString());
            }
        });


        layout.setBackgroundColor(Color.parseColor("#FFFFFF"));

        layout.addView(webViewContainer);

        // Barra inferior contendo o botão, com fundo branco
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.VERTICAL);
        bottomBar.setBackgroundColor(Color.parseColor("#FFFFFF"));
        int paddingPx = (int) (8 * getResources().getDisplayMetrics().density);
        bottomBar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bottomBar.setLayoutParams(bottomParams);
        bottomBar.addView(closeButton);

        layout.addView(bottomBar);
        setContentView(layout);

        String url = getIntent().getStringExtra("url");
        appUUID = extractUUIDFromUrl(url);
        
        Log.d(TAG, "URL: " + url + ", buttonText: " + buttonText);
        
        // Definir cookie com o UUID
        if (url != null && !appUUID.isEmpty()) {
            CookieManager.getInstance().setCookie(url, "UUID=" + appUUID + "; Path=/");
        }
        
        if (url != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Se a Activity está terminando E o deeplink NÃO foi tratado, 
        // envie um resultado de CANCELAMENTO.
        if (isFinishing() && getCallingActivity() != null && !deeplinkHandled) { 
            setResult(Activity.RESULT_CANCELED);
        }
        
        //WebViewPlugin.sendEvent("onWebViewClosed", ""); // ou envie dados, se quiser
    }
    /*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // garante que getIntent() esteja atualizado


        Uri data = intent.getData();
        Log.d("DEEPLINK", "Received deep link: " + data.toString());

        if (data != null && webView != null) {
            WebViewPlugin.sendEvent("onDeeplinkCalled", data.toString());
            // Se quiser encerrar a WebView após o evento:
            runOnUiThread(() -> finish());
            //webView.loadUrl(data.toString());
        }
    }
    */

    private void handleClientCertRequest(ClientCertRequest request) {
        final String host = request.getHost() == null ? "" : request.getHost();

        if (clientCertAlias == null || clientCertAlias.trim().isEmpty()) {
            Log.d(TAG, "Client cert requested, but no alias was configured");
            WebViewPlugin.sendEvent("clientcert_skipped", "alias_missing");
            request.ignore();
            return;
        }

        if (!isHostAllowed(host)) {
            Log.w(TAG, "Client cert denied for non-allowed host: " + host);
            WebViewPlugin.sendEvent("clientcert_denied_host", host);
            request.cancel();
            return;
        }

        if (clientCertEnabledByTrigger && !clientCertArmed) {
            Log.d(TAG, "Client cert requested before trigger for host: " + host);
            WebViewPlugin.sendEvent("clientcert_waiting_trigger", host);
            request.ignore();
            return;
        }

        final String alias = clientCertAlias.trim();
        new Thread(() -> {
            try {
                PrivateKey privateKey = KeyChain.getPrivateKey(getApplicationContext(), alias);
                X509Certificate[] certChain = KeyChain.getCertificateChain(getApplicationContext(), alias);

                runOnUiThread(() -> {
                    if (privateKey != null && certChain != null && certChain.length > 0) {
                        Log.d(TAG, "Client cert provided for host: " + host + " with alias: " + alias);
                        request.proceed(privateKey, certChain);
                        WebViewPlugin.sendEvent("clientcert_proceed", host);
                        if (clientCertEnabledByTrigger) {
                            clientCertArmed = false;
                        }
                    } else {
                        Log.e(TAG, "Client cert alias not found or empty chain: " + alias);
                        WebViewPlugin.sendEvent("clientcert_error", "alias_not_found");
                        request.cancel();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error resolving client cert alias: " + e.getMessage());
                    WebViewPlugin.sendEvent("clientcert_error", "keychain_error");
                    request.cancel();
                });
            }
        }).start();
    }

    private boolean isHostAllowed(String host) {
        if (clientCertAllowedHosts.isEmpty()) {
            return true;
        }
        if (host == null || host.trim().isEmpty()) {
            return false;
        }
        return clientCertAllowedHosts.contains(host.trim().toLowerCase(Locale.US));
    }

    private String extractUrlFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            return data.toString(); // Para deep links tipo myapp://...
        }
        return intent.getStringExtra("url");
    }

    // Helper para extrair UUID da URL
    private String extractUUIDFromUrl(String url) {
        if (url == null) return "";
        try {
            Uri uri = Uri.parse(url);
            String uuid = uri.getQueryParameter("UUID");
            if (uuid != null) {
                Log.d(TAG, "UUID extraído: " + uuid);
                return uuid;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao extrair UUID: " + e.getMessage());
        }
        return "";
    }
}