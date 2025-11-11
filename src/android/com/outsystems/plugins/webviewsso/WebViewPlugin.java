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

public class WebViewPlugin extends CordovaPlugin {

    //private static CallbackContext eventCallback;
    
    // Guarde o CallbackContext original (apenas para o openWebView)
    private CallbackContext openWebViewCallback; 
    private static CallbackContext eventCallback; // Mantenha para eventos globais

    // Códigos de requisição
    private static final int WEBVIEW_REQUEST_CODE = 1001;
    

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openWebView")) {
            String url = args.getString(0);
            openWebView(url, callbackContext);
            return true;
        } else if (action.equals("registerEventListener")) {
            eventCallback = callbackContext;
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            eventCallback.sendPluginResult(pluginResult);
            return true;
        }
        return false;
    }

    private void openWebView(String url, CallbackContext callbackContext) {
        
        // 1. Salva o callbackContext da chamada original.
        this.openWebViewCallback = callbackContext;
        
        Intent intent = new Intent(cordova.getActivity(), WebViewActivity.class);
        intent.putExtra("url", url);
        
        // 2. Inicia a Activity esperando um resultado.
        cordova.startActivityForResult(this, intent, WEBVIEW_REQUEST_CODE);
        
        //cordova.getActivity().startActivity(intent);
        //callbackContext.success();

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true); // Deixa o callback vivo até onActivityResult
        callbackContext.sendPluginResult(pluginResult);
        
    }
    
    // 3. Adiciona onActivityResult para receber o resultado da WebViewActivity
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == WEBVIEW_REQUEST_CODE && this.openWebViewCallback != null) {
            
            // O resultado (DeepLink) deve estar no Intent.
            String deepLinkUrl = intent != null ? intent.getStringExtra("deeplink_result") : null;

            if (resultCode == Activity.RESULT_OK && deepLinkUrl != null) {
                // Evento de sucesso, com o URL do Deeplink.
                this.openWebViewCallback.success(deepLinkUrl);
            } else {
                // Evento de cancelamento ou erro.
                this.openWebViewCallback.error("WebView closed or cancelled.");
            }
            
            // O callback só é usado uma vez, não precisa mais do keepCallback(true).
            this.openWebViewCallback = null; 
        }
    }
    
    public static void sendEvent(String type, String data) {
        if (eventCallback != null) {
            try {
                JSONObject event = new JSONObject();
                event.put("type", type);
                event.put("url", data);
                PluginResult result = new PluginResult(PluginResult.Status.OK, event);
                result.setKeepCallback(true);
                eventCallback.sendPluginResult(result);
            } catch (JSONException e) {
                // ignore
            }
        }
    }
}
