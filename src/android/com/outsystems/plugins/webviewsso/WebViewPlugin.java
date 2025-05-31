package com.outsystems.plugins.webviewsso;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.Intent;

public class WebViewPlugin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openWebView")) {
            Intent intent = new Intent(cordova.getActivity(), WebViewActivity.class);
            intent.putExtra("url", args.getString(0));
            cordova.getActivity().startActivity(intent);
            callbackContext.success("WebView opened");
            return true;
        }
        return false;
    }
}