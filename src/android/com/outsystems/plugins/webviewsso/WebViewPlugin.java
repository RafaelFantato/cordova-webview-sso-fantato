package com.outsystems.plugins.webviewsso;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class WebViewPlugin extends CordovaPlugin {

    //private static CallbackContext eventCallback;
    
    // Guarde o CallbackContext original (apenas para o openWebView)
    private CallbackContext openWebViewCallback; 
    private static CallbackContext eventCallback; // Mantenha para eventos globais

    // Códigos de requisição
    private static final int WEBVIEW_REQUEST_CODE = 1001;
    
    // Defina uma TAG constante para facilitar a filtragem no Logcat
    private static final String TAG = "WebViewActivitySSO";
    

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute called with action=" + action + ", argsLength=" + args.length());
        if (action.equals("openWebView")) {
            String url = args.getString(0);
            JSONObject options = args.length() > 1 ? args.getJSONObject(1) : new JSONObject();
            Log.d(TAG, "openWebView requested for url=" + url + ", options=" + options.toString());
            openWebView(url, options, callbackContext);
            return true;
        } else if (action.equals("registerEventListener")) {
            eventCallback = callbackContext;
            Log.d(TAG, "registerEventListener called. Global event callback registered");
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            eventCallback.sendPluginResult(pluginResult);
            return true;
        }
        Log.w(TAG, "Unknown action received: " + action);
        return false;
    }

    private void openWebView(String url, JSONObject options, CallbackContext callbackContext) {
        
        // 1. Salva o callbackContext da chamada original.
        this.openWebViewCallback = callbackContext;
        Log.d(TAG, "Preparing WebViewActivity intent for url=" + url);
        
        Intent intent = new Intent(cordova.getActivity(), WebViewActivity.class);
        intent.putExtra("url", url);
        
        // 2. Extrai buttonText das opções (se fornecido)
        try {
            if (options.has("buttonText")) {
                String buttonText = options.getString("buttonText");
                intent.putExtra("buttonText", buttonText);
                Log.d(TAG, "buttonText extraído das opções: " + buttonText);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao extrair buttonText: " + e.getMessage());
        }
        
        // 2. Extrai PlatformVersion das opções (se fornecido)
        try {
            if (options.has("PlatformVersion")) {
                String PlatformVersion = options.getString("PlatformVersion");
                intent.putExtra("PlatformVersion", PlatformVersion);
                Log.d(TAG, "PlatformVersion extraído das opções: " + PlatformVersion);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao extrair PlatformVersion: " + e.getMessage());
        }

        // 3. Opções de client certificate (mTLS)
        try {
            if (options.has("clientCertAlias")) {
                String clientCertAlias = options.getString("clientCertAlias");
                intent.putExtra("clientCertAlias", clientCertAlias);
                Log.d(TAG, "clientCertAlias recebido: " + clientCertAlias);
            }

            boolean enabledByTrigger = true;
            if (options.has("clientCertEnabledByTrigger")) {
                enabledByTrigger = options.getBoolean("clientCertEnabledByTrigger");
            }
            intent.putExtra("clientCertEnabledByTrigger", enabledByTrigger);
            Log.d(TAG, "clientCertEnabledByTrigger=" + enabledByTrigger);

            boolean allowSystemSelection = true;
            if (options.has("clientCertAllowSystemSelection")) {
                allowSystemSelection = options.getBoolean("clientCertAllowSystemSelection");
            }
            intent.putExtra("clientCertAllowSystemSelection", allowSystemSelection);
            Log.d(TAG, "clientCertAllowSystemSelection=" + allowSystemSelection);

            if (options.has("clientCertAllowedHosts")) {
                JSONArray hostsArray = options.getJSONArray("clientCertAllowedHosts");
                ArrayList<String> hosts = new ArrayList<>();
                for (int i = 0; i < hostsArray.length(); i++) {
                    String host = hostsArray.optString(i, "");
                    if (!host.isEmpty()) {
                        hosts.add(host);
                    }
                }
                intent.putStringArrayListExtra("clientCertAllowedHosts", hosts);
                Log.d(TAG, "clientCertAllowedHosts configurado com " + hosts.size() + " item(ns)");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Erro ao extrair opções de client certificate: " + e.getMessage());
        }

        // 4. Inicia a Activity esperando um resultado.
        Log.d(TAG, "Starting WebViewActivity for result. requestCode=" + WEBVIEW_REQUEST_CODE);
        cordova.startActivityForResult(this, intent, WEBVIEW_REQUEST_CODE);
        
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
        
    }
    
    // 3. Adiciona onActivityResult para receber o resultado da WebViewActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == WEBVIEW_REQUEST_CODE && this.openWebViewCallback != null) {
            Log.d(TAG, "onActivityResult received. resultCode=" + resultCode + ", hasIntent=" + (intent != null));
            
            // O resultado (DeepLink) deve estar no Intent.
            String deepLinkUrl = intent != null ? intent.getStringExtra("deeplink_result") : null;
            Log.d(TAG, "deeplink_result=" + deepLinkUrl);

            if (resultCode == Activity.RESULT_OK && deepLinkUrl != null) {
                // Evento de sucesso, com o URL do Deeplink.
                Log.d(TAG, "Returning success to JS callback with deeplink=" + deepLinkUrl);
                this.openWebViewCallback.success(deepLinkUrl);
            } else {
                // Evento de cancelamento ou erro.
                Log.w(TAG, "Returning error to JS callback: WebView closed or cancelled");
                this.openWebViewCallback.error("WebView closed or cancelled.");
            }
            
            // O callback só é usado uma vez, não precisa mais do keepCallback(true).
            this.openWebViewCallback = null; 
        }
    }
    
    public static void sendEvent(String type, String data) {
        if (eventCallback != null) {
            try {
                Log.d(TAG, "sendEvent type=" + type + ", data=" + data);
                JSONObject event = new JSONObject();
                event.put("type", type);
                event.put("url", data);
                PluginResult result = new PluginResult(PluginResult.Status.OK, event);
                result.setKeepCallback(true);
                eventCallback.sendPluginResult(result);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to serialize event " + type + ": " + e.getMessage());
            }
        } else {
            Log.d(TAG, "sendEvent skipped because eventCallback is null. type=" + type + ", data=" + data);
        }
    }
}