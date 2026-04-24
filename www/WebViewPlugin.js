var exec = require('cordova/exec');

var WebViewPlugin = {
  /**
   * Abre uma nova WebView com a URL fornecida.
   *
   * Opções suportadas:
   * - buttonText: string
   * - PlatformVersion: string
   * - clientCertAlias: string (alias instalado no Android KeyChain)
   * - clientCertEnabledByTrigger: boolean (default true)
   * - clientCertAllowedHosts: string[] (hosts permitidos para mTLS)
   *
   * Quando clientCertEnabledByTrigger=true, a página carregada precisa chamar:
   * window.WebViewPluginNative.armClientCertificate()
   * antes da chamada HTTPS que requer client certificate.
   *
   * @param {string} url - URL a ser carregada
   * @param {object} options - opções adicionais
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