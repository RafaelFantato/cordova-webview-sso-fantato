package cordova.plugin.webviewplugin;

import android.app.Activity;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.util.Log;
import org.apache.cordova.*;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebViewPlugin extends CordovaPlugin {

    private static CallbackContext eventCallback;

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
        Intent intent = new Intent(cordova.getActivity(), WebViewActivity.class);
        intent.putExtra("url", url);
        cordova.getActivity().startActivity(intent);
        callbackContext.success();
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