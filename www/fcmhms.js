var exec = require('cordova/exec');

exports.isGMS = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "isGMS", []);
};

exports.isHMS = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "isHMS", []);
};

exports.initFcmHms = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "initFcmHms", []);
};

exports.initPerformance = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "initPerformance", []);
};

exports.getVerificationID = function (number, success, error) {
  exec(success, error, "FCMHMSPlugin", "getVerificationID", [number]);
};

exports.getInstanceId = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "getInstanceId", []);
};

exports.getId = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "getId", []);
};

exports.getToken = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "getToken", []);
};

exports.onNotificationOpen = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "onNotificationOpen", []);
};

exports.onTokenRefresh = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "onTokenRefresh", []);
};

exports.grantPermission = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "grantPermission", []);
};

exports.hasPermission = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "hasPermission", []);
};

exports.setBadgeNumber = function (number, success, error) {
  exec(success, error, "FCMHMSPlugin", "setBadgeNumber", [number]);
};

exports.getBadgeNumber = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "getBadgeNumber", []);
};

exports.subscribe = function (topic, success, error) {
  exec(success, error, "FCMHMSPlugin", "subscribe", [topic]);
};

exports.unsubscribe = function (topic, success, error) {
  exec(success, error, "FCMHMSPlugin", "unsubscribe", [topic]);
};

exports.unregister = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "unregister", []);
};

exports.getByteArray = function (key, namespace, success, error) {
  var args = [key];
  if (typeof namespace === 'string') {
    args.push(namespace);
  } else {
    error = success;
    success = namespace;
  }
  exec(success, error, "FCMHMSPlugin", "getByteArray", args);
};

exports.clearAllNotifications = function (success, error) {
  exec(success, error, "FCMHMSPlugin", "clearAllNotifications", []);
};