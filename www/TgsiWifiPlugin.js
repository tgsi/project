cordova.define("cordova-plugin-tgsi-wifi.TgsiWifiPlugin", function(require, exports, module) {
function TgsiWifiPlugin() {
}

TgsiWifiPlugin.prototype.PluginName = "TgsiWifiPlugin";

TgsiWifiPlugin.prototype.pInvoke = function (method, data, successCallback, errorCallback) {
    if(data == null || data === undefined) {
        data = [];
    } else if (!Array.isArray(data)) {
        data = [data];
    }
  cordova.exec(successCallback, errorCallback, this.PluginName, method, data);
};

TgsiWifiPlugin.prototype.coolMethod =  function (data, successCallback, errorCallback) {
    this.pInvoke("coolMethod", data, successCallback, errorCallback);
};

TgsiWifiPlugin.prototype.registerService =  function (data, successCallback, errorCallback) {
    this.pInvoke("registerService", data, successCallback, errorCallback);
};

TgsiWifiPlugin.prototype.startSearching =  function (data, successCallback, errorCallback) {
    this.pInvoke("startSearching", data, successCallback, errorCallback);
};

TgsiWifiPlugin.prototype.getPeerList =  function (data, successCallback, errorCallback) {
    this.pInvoke("getPeerList", data, successCallback, errorCallback);
};

TgsiWifiPlugin.prototype.connect =  function (data, successCallback, errorCallback) {
    this.pInvoke("connect", data, successCallback, errorCallback);
};

TgsiWifiPlugin.prototype.sendMessage =  function (data, successCallback, errorCallback) {
    this.pInvoke("sendMessage", data, successCallback, errorCallback);
};

TgsiWifiPlugin.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.TgsiWifiPlugin = new TgsiWifiPlugin();
  return window.plugins.TgsiWifiPlugin;
};

cordova.addConstructor(TgsiWifiPlugin.install);


});
