var exec = require('cordova/exec');

exports.openWebView = function (url, success, error) {
    exec(success, error, 'WebViewPlugin', 'openWebView', [url]);
};