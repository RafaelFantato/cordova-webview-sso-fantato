var exec = require('cordova/exec');

var WebViewPlugin = {
  /**
   * Abre uma nova WebView com a URL fornecida
   * @param {string} url - URL a ser carregada
   * @param {object} options - opções adicionais (placeholder para expandir)
   * @param {function} successCallback - callback chamado em sucesso
   * @param {function} errorCallback - callback chamado em erro
   */
  openWebView: function (url, options = {}, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "WebViewPlugin", "openWebView", [url, options]);
  },

  /**
   * Registra um listener de evento vindo da WebView nativa
   * @param {function} callback - callback para eventos como 'loadstart', 'loaderror', 'exit'
   */
  onEvent: function (callback) {
    exec(callback, null, "WebViewPlugin", "registerEventListener", []);
  }
};

module.exports = WebViewPlugin;