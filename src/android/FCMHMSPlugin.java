package org.apache.cordova.fcmhms;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;
import androidx.annotation.NonNull;

import com.huawei.hms.api.HuaweiApiAvailability;
import com.google.android.gms.common.GoogleApiAvailability;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigInfo;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;

import me.leolin.shortcutbadger.ShortcutBadger;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Firebase PhoneAuth
import java.util.concurrent.TimeUnit;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;

public class FCMHMSPlugin extends CordovaPlugin {

    private FirebaseAnalytics mFirebaseAnalytics;
    private static CordovaWebView appView;
    private final String TAG = "FCMHMSPlugin";
    private final String ERRORISGMS = "Cannot get isGMS";
    private final String ERRORISHMS = "Cannot get isHMS";
    private final String ERRORINIT = "FCM or GMS aren't initialised";
    private final String ERRORINITCRASHLYTICS = "Crashlytics isn't initialised";
    private final String ERRORINITANALYTICS = "Analytics isn't initialised";
    private final String ERRORINITREMOTECONFIG = "RemoteConfig isn't initialised";
    private final String ERRORINITPERFORMANCE = "Performance isn't initialised";
    protected static final String KEY = "badge";

    private static boolean gmsAvailability = false;
    private static boolean hmsAvailability = false;
    private static boolean firebaseInit = false;
    private static boolean hmsInit = false;
    private static boolean crashlyticsInit = false;
    private static boolean analyticsInit = false;
    private static boolean remoteconfigInit = false;
    private static boolean performanceInit = false;
    private static boolean inBackground = true;
    private static ArrayList<Bundle> notificationStack = null;
    private static CallbackContext notificationCallbackContext;
    private static CallbackContext tokenRefreshCallbackContext;

    @Override
    protected void pluginInitialize() {
        final Bundle extras = this.cordova.getActivity().getIntent().getExtras();
        this.cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Log.d(TAG, "Starting fcmhms plugin");
                Context context = cordova.getContext();
                if (null != context) {
                    int gms = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
                    FCMHMSPlugin.gmsAvailability = (com.google.android.gms.common.ConnectionResult.SUCCESS == gms);
                    if(!FCMHMSPlugin.gmsAvailability){
                      int hms = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context);
                      FCMHMSPlugin.hmsAvailability = (com.huawei.hms.api.ConnectionResult.SUCCESS == hms);
                    }
                }
                if (extras != null && extras.size() > 1) {
                    if (FCMHMSPlugin.notificationStack == null) {
                        FCMHMSPlugin.notificationStack = new ArrayList<Bundle>();
                    }
                    if (extras.containsKey("google.message_id")) {
                        extras.putBoolean("tap", true);
                        notificationStack.add(extras);
                    }
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("isGMS")) {
          this.isGMS(callbackContext);
          return true;
        } else if (action.equals("isHMS")) {
          this.isHMS(callbackContext);
          return true;
        } else if (action.equals("initFcmHms")) {
          this.initFcmHms(callbackContext);
          return true;
        } else if (action.equals("initCrashlytics")) {
          this.initCrashlytics(callbackContext);
          return true;
        } else if (action.equals("initAnalytics")) {
          this.initAnalytics(callbackContext);
          return true;
        } else if (action.equals("initPerformance")) {
          this.initPerformance(callbackContext);
          return true;
        } else if (action.equals("getInstanceId")) {
            this.getInstanceId(callbackContext);
            return true;
        } else if (action.equals("getId")) {
            this.getId(callbackContext);
            return true;
        } else if (action.equals("getToken")) {
            this.getToken(callbackContext);
            return true;
        } else if (action.equals("hasPermission")) {
            this.hasPermission(callbackContext);
            return true;
        } else if (action.equals("setBadgeNumber")) {
            this.setBadgeNumber(callbackContext, args.getInt(0));
            return true;
        } else if (action.equals("getBadgeNumber")) {
            this.getBadgeNumber(callbackContext);
            return true;
        } else if (action.equals("subscribe")) {
            this.subscribe(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("unsubscribe")) {
            this.unsubscribe(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("unregister")) {
            this.unregister(callbackContext);
            return true;
        } else if (action.equals("onNotificationOpen")) {
            this.onNotificationOpen(callbackContext);
            return true;
        } else if (action.equals("onTokenRefresh")) {
            this.onTokenRefresh(callbackContext);
            return true;
        } else if (action.equals("logEvent")) {
            this.logEvent(callbackContext, args.getString(0), args.getJSONObject(1));
            return true;
        } else if (action.equals("logError")) {
            this.logError(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setScreenName")) {
            this.setScreenName(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setUserId")) {
            this.setUserId(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setUserProperty")) {
            this.setUserProperty(callbackContext, args.getString(0), args.getString(1));
            return true;
        } else if (action.equals("activateFetched") || action.equals("initRemoteConfig")) {
            this.activateFetched(callbackContext);
            return true;
        } else if (action.equals("fetch")) {
            if (args.length() > 0) {
                this.fetch(callbackContext, args.getLong(0));
            } else {
                this.fetch(callbackContext);
            }
            return true;
        } /*else if (action.equals("getByteArray")) {
            this.getByteArray(callbackContext, args.getString(0));
            return true;
        }*/ else if (action.equals("getValue")) {
            this.getValue(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("getInfo")) {
            this.getInfo(callbackContext);
            return true;
        } else if (action.equals("setConfigSettings")) {
            this.setConfigSettings(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("setDefaults")) {
            this.setDefaults(callbackContext, args.getJSONObject(0));
            return true;
        } else if (action.equals("startTrace")) {
            this.startTrace(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("incrementCounter")) {
            this.incrementCounter(callbackContext, args.getString(0), args.getString(1));
            return true;
        } else if (action.equals("stopTrace")) {
            this.stopTrace(callbackContext, args.getString(0));
            return true;
        } else if (action.equals("setAnalyticsCollectionEnabled")) {
            this.setAnalyticsCollectionEnabled(callbackContext, args.getBoolean(0));
            return true;
        } else if (action.equals("setPerformanceCollectionEnabled")) {
          this.setPerformanceCollectionEnabled(callbackContext, args.getBoolean(0));
          return true;
        } else if (action.equals("clearAllNotifications")) {
            this.clearAllNotifications(callbackContext);
            return true;
        }

        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        FCMHMSPlugin.inBackground = true;
    }

    @Override
    public void onResume(boolean multitasking) {
        FCMHMSPlugin.inBackground = false;
    }

    @Override
    public void onReset() {
        FCMHMSPlugin.notificationCallbackContext = null;
        FCMHMSPlugin.tokenRefreshCallbackContext = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.exit(0);

        if (this.appView != null) {
            appView.handleDestroy();
        }
    }

    private void isGMS(final CallbackContext callbackContext) {
        Log.d(TAG, "isGMS");
        try {
          PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, FCMHMSPlugin.gmsAvailability);
          pluginresult.setKeepCallback(true);
          callbackContext.sendPluginResult(pluginresult);
        } catch(Exception e) {
          callbackContext.error(ERRORISGMS);
        }
    }

    private void isHMS(final CallbackContext callbackContext) {
        Log.d(TAG, "isHMS");
        try {
          PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, FCMHMSPlugin.hmsAvailability);
          pluginresult.setKeepCallback(true);
          callbackContext.sendPluginResult(pluginresult);
        } catch(Exception e) {
          callbackContext.error(ERRORISHMS);
        }
    }

    private void initFcmHms(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Firebase");
        try {
          if(FCMHMSPlugin.gmsAvailability){
            FirebaseApp.initializeApp(context);
            FCMHMSPlugin.firebaseInit = true;
          } else {
            String appId = AGConnectServicesConfig.fromContext(cordova.getContext()).getString("client/app_id");
            FCMHMSPlugin.hmsInit = true;
          }
          callbackContext.success();
        } catch(Exception e) {
          if(FCMHMSPlugin.crashlyticsInit()){
            Crashlytics.logException(e);
          }
          callbackContext.error(ERRORINIT);
        }
    }

    private void initCrashlytics(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Crashlytics");
        try {
          Fabric.with(context, new Crashlytics());
          FCMHMSPlugin.crashlyticsInit = true;
          callbackContext.success();
        } catch(Exception e) {
          callbackContext.error(ERRORINITCRASHLYTICS);
        }
    }

    private void initAnalytics(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Analytics");
        try {
          mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
          mFirebaseAnalytics.setAnalyticsCollectionEnabled(true);
          FCMHMSPlugin.analyticsInit = true;
          callbackContext.success();
        } catch(Exception e) {
          if(FCMHMSPlugin.crashlyticsInit()){
            Crashlytics.logException(e);
          }
          callbackContext.error(ERRORINITANALYTICS);
        }
    }

    private void initPerformance(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Performance");
        try {
          FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
          FCMHMSPlugin.performanceInit = true;
          callbackContext.success();
        } catch(Exception e) {
          if(FCMHMSPlugin.crashlyticsInit()){
            Crashlytics.logException(e);
          }
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void onNotificationOpen(final CallbackContext callbackContext) {
        FCMHMSPlugin.notificationCallbackContext = callbackContext;
        if (FCMHMSPlugin.notificationStack != null) {
            for (Bundle bundle : FCMHMSPlugin.notificationStack) {
                FCMHMSPlugin.sendNotification(bundle, this.cordova.getActivity().getApplicationContext());
            }
            FCMHMSPlugin.notificationStack.clear();
        }
    }

    private void onTokenRefresh(final CallbackContext callbackContext) {
      final FCMHMSPlugin self = this;
        FCMHMSPlugin.tokenRefreshCallbackContext = callbackContext;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(FCMHMSPlugin.firebaseInit()){
                      FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( self.cordova.getActivity(),  new OnSuccessListener<InstanceIdResult>() {
                          @Override
                          public void onSuccess(InstanceIdResult instanceIdResult) {
                                String token = instanceIdResult.getToken();
                                if (token != null) {
                                    FCMHMSPlugin.sendToken(token);
                                    Log.d(TAG, "onTokenRefresh Token : "+token);
                                } else {
                                    Log.d(TAG, "onTokenRefresh failed");
                                }
                          }
                      });
                    } else {
                      callbackContext.error(ERRORINIT);
                    }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    public static void sendNotification(Bundle bundle, Context context) {
        if (!FCMHMSPlugin.hasNotificationsCallback()) {
            String packageName = context.getPackageName();
            if (FCMHMSPlugin.notificationStack == null) {
                FCMHMSPlugin.notificationStack = new ArrayList<Bundle>();
            }
            notificationStack.add(bundle);

            /* start the main activity, if not running */
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(packageName, packageName + ".MainActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("cdvStartInBackground", true);

            context.startActivity(intent);

            return;
        }
        final CallbackContext callbackContext = FCMHMSPlugin.notificationCallbackContext;
        if (callbackContext != null && bundle != null) {
            JSONObject json = new JSONObject();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                try {
                    json.put(key, bundle.get(key));
                } catch (JSONException e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                    return;
                }
            }

            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, json);
            pluginresult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginresult);
        }
    }

    public static void sendToken(String token) {
        if (FCMHMSPlugin.tokenRefreshCallbackContext == null) {
            return;
        }

        final CallbackContext callbackContext = FCMHMSPlugin.tokenRefreshCallbackContext;
        if (callbackContext != null && token != null) {
            PluginResult pluginresult = new PluginResult(PluginResult.Status.OK, token);
            pluginresult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginresult);
        }
    }

    public static boolean inBackground() {
        return FCMHMSPlugin.inBackground;
    }

    public static boolean firebaseInit() {
        return FCMHMSPlugin.firebaseInit;
    }

    public static boolean hmsInit() {
        return FCMHMSPlugin.hmsInit;
    }

    public static boolean fcmgmsInit() {
        return FCMHMSPlugin.firebaseInit || FCMHMSPlugin.hmsInit;
    }

    public static boolean crashlyticsInit() {
        return FCMHMSPlugin.crashlyticsInit;
    }

    public static boolean analyticsInit() {
        return FCMHMSPlugin.analyticsInit;
    }

    public static boolean remoteconfigInit() {
        return FCMHMSPlugin.remoteconfigInit;
    }

    public static boolean performanceInit() {
        return FCMHMSPlugin.performanceInit;
    }

    public static boolean hasNotificationsCallback() {
        return FCMHMSPlugin.notificationCallbackContext != null;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final Bundle data = intent.getExtras();
        if (data != null && data.containsKey("google.message_id")) {
            data.putBoolean("tap", true);
            FCMHMSPlugin.sendNotification(data, this.cordova.getActivity().getApplicationContext());
        }
    }

    // DEPRECTED - alias of getToken
    private void getInstanceId(final CallbackContext callbackContext) {
        final FCMHMSPlugin self = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(FCMHMSPlugin.firebaseInit()){
                      FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( self.cordova.getActivity(),  new OnSuccessListener<InstanceIdResult>() {
                          @Override
                          public void onSuccess(InstanceIdResult instanceIdResult) {
                                String token = instanceIdResult.getToken();
                                if (token != null) {
                                    callbackContext.success(token);
                                    Log.d(TAG, "getInstanceId Token : "+token);
                                } else {
                                    Log.d(TAG, "getInstanceId failed");
                                }
                          }
                      });
                    } else {
                      callbackContext.error(ERRORINIT);
                    }
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                  if(FCMHMSPlugin.firebaseInit()){
                    String id = FirebaseInstanceId.getInstance().getId();
                    callbackContext.success(id);
                  } else {
                    callbackContext.error(ERRORINIT);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getToken(final CallbackContext callbackContext) {
        final FCMHMSPlugin self = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                  if(FCMHMSPlugin.firebaseInit()){
                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener( self.cordova.getActivity(),  new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                              String token = instanceIdResult.getToken();
                              if (token != null) {
                                  callbackContext.success(token);
                                  Log.d(TAG, "getInstanceId Token : "+token);
                              } else {
                                  Log.d(TAG, "getInstanceId failed");
                              }
                        }
                    });
                  } else if(FCMHMSPlugin.hmsInit()){
                    String appId = AGConnectServicesConfig.fromContext(cordova.getContext()).getString("client/app_id");
                    Log.i(TAG, "[getToken] appId = " + appId);
                    String pushToken = HmsInstanceId.getInstance(cordova.getContext()).getToken(appId, "HCM");
                    if (!TextUtils.isEmpty(pushToken)) {
                        Log.i(TAG, "[getToken] token is received");
                        callbackContext.success(pushToken);
                    }
                  } else {
                    callbackContext.error(ERRORINIT);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void hasPermission(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
                    boolean areNotificationsEnabled = notificationManagerCompat.areNotificationsEnabled();
                    JSONObject object = new JSONObject();
                    object.put("isEnabled", areNotificationsEnabled);
                    callbackContext.success(object);
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setBadgeNumber(final CallbackContext callbackContext, final int number) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    SharedPreferences.Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
                    editor.putInt(KEY, number);
                    editor.apply();
                    ShortcutBadger.applyCount(context, number);
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void getBadgeNumber(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    SharedPreferences settings = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
                    int number = settings.getInt(KEY, 0);
                    callbackContext.success(number);
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void subscribe(final CallbackContext callbackContext, final String topic) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic);
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void unsubscribe(final CallbackContext callbackContext, final String topic) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void unregister(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                  if(FCMHMSPlugin.firebaseInit()){
                    FirebaseInstanceId.getInstance().deleteInstanceId();
                    callbackContext.success();
                  } else {
                    callbackContext.error(ERRORINIT);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void logEvent(final CallbackContext callbackContext, final String name, final JSONObject params)
            throws JSONException {
        final Bundle bundle = new Bundle();
        Iterator iter = params.keys();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Object value = params.get(key);

            if (value instanceof Integer || value instanceof Double) {
                bundle.putFloat(key, ((Number) value).floatValue());
            } else {
                bundle.putString(key, value.toString());
            }
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(FCMHMSPlugin.analyticsInit()){
                      mFirebaseAnalytics.logEvent(name, bundle);
                      callbackContext.success();
                    } else {
                      callbackContext.error(ERRORINITANALYTICS);
                    }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void logError(final CallbackContext callbackContext, final String message) throws JSONException {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(new Exception(message));
                      callbackContext.success(1);
                    } else {
                      callbackContext.error(ERRORINITCRASHLYTICS);
                    }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.log(e.getMessage());
                    }
                    e.printStackTrace();
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setScreenName(final CallbackContext callbackContext, final String name) {
        // This must be called on the main thread
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                try {
                  if(FCMHMSPlugin.analyticsInit()){
                    mFirebaseAnalytics.setCurrentScreen(cordova.getActivity(), name, null);
                    callbackContext.success();
                  } else {
                    callbackContext.error(ERRORINITANALYTICS);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setUserId(final CallbackContext callbackContext, final String id) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                  if(FCMHMSPlugin.analyticsInit()){
                    mFirebaseAnalytics.setUserId(id);
                    callbackContext.success();
                  } else {
                    callbackContext.error(ERRORINITANALYTICS);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void setUserProperty(final CallbackContext callbackContext, final String name, final String value) {
      if(FCMHMSPlugin.analyticsInit()){
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    mFirebaseAnalytics.setUserProperty(name, value);
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITANALYTICS);
      }
    }

    private void activateFetched(final CallbackContext callbackContext) {
      cordova.getThreadPool().execute(new Runnable() {
          public void run() {
              try {
                if (!FCMHMSPlugin.remoteconfigInit()) {
                  FirebaseRemoteConfig.getInstance().activate()
                    .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Boolean>() {
                        @Override
                        public void onComplete(@NonNull Task<Boolean> task) {
                            final boolean activated = task.isSuccessful();
                            FCMHMSPlugin.remoteconfigInit = true;
                            callbackContext.success(String.valueOf(activated));
                        }
                    });
                } else {
                  callbackContext.error(String.valueOf(true));
                }
              } catch (Exception e) {
                  if(FCMHMSPlugin.crashlyticsInit()){
                    Crashlytics.logException(e);
                  }
                  callbackContext.error(e.getMessage());
              }
          }
      });
  }

    private void fetch(CallbackContext callbackContext) {
        if (FCMHMSPlugin.remoteconfigInit()) {
          fetch(callbackContext, FirebaseRemoteConfig.getInstance().fetch());
        } else {
          callbackContext.error(ERRORINITREMOTECONFIG);
        }
    }

    private void fetch(CallbackContext callbackContext, long cacheExpirationSeconds) {
        if (FCMHMSPlugin.remoteconfigInit()) {
          fetch(callbackContext, FirebaseRemoteConfig.getInstance().fetch(cacheExpirationSeconds));
        } else {
          callbackContext.error(ERRORINITREMOTECONFIG);
        }
    }

    private void fetch(final CallbackContext callbackContext, final Task<Void> task) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    task.addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            callbackContext.success();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            if(FCMHMSPlugin.crashlyticsInit()){
                              Crashlytics.logException(e);
                            }
                            callbackContext.error(e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    /*private void getByteArray(final CallbackContext callbackContext, final String key) {
      if (FCMHMSPlugin.remoteconfigInit()) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    byte[] bytes = FirebaseRemoteConfig.getInstance().getByteArray(key);
                    JSONObject object = new JSONObject();
                    object.put("base64", Base64.encodeToString(bytes, Base64.DEFAULT));
                    object.put("array", new JSONArray(bytes));
                    callbackContext.success(object);
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITREMOTECONFIG);
      }
    }*/

    private void getValue(final CallbackContext callbackContext, final String key) {
      if (FCMHMSPlugin.remoteconfigInit()) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseRemoteConfigValue value = FirebaseRemoteConfig.getInstance().getValue(key);
                    callbackContext.success(value.asString());
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITREMOTECONFIG);
      }
    }

    private void getInfo(final CallbackContext callbackContext) {
      if (FCMHMSPlugin.remoteconfigInit()) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseRemoteConfigInfo remoteConfigInfo = FirebaseRemoteConfig.getInstance().getInfo();
                    JSONObject info = new JSONObject();

                    JSONObject settings = new JSONObject();
                    settings.put("developerModeEnabled", new FirebaseRemoteConfigSettings.Builder().getMinimumFetchIntervalInSeconds() == 0);
                    info.put("configSettings", settings);

                    info.put("fetchTimeMillis", remoteConfigInfo.getFetchTimeMillis());
                    info.put("lastFetchStatus", remoteConfigInfo.getLastFetchStatus());

                    callbackContext.success(info);
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITREMOTECONFIG);
      }
    }

    private void setConfigSettings(final CallbackContext callbackContext, final JSONObject config) {
      if (FCMHMSPlugin.remoteconfigInit()) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    boolean devMode = config.getBoolean("developerModeEnabled");
                    FirebaseRemoteConfigSettings.Builder settings = new FirebaseRemoteConfigSettings.Builder()
                            .setMinimumFetchIntervalInSeconds(devMode ? 0 : 3600);
                    FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(settings.build());
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITREMOTECONFIG);
      }
    }

    private void setDefaults(final CallbackContext callbackContext, final JSONObject defaults) {
      if (FCMHMSPlugin.remoteconfigInit()) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    FirebaseRemoteConfig.getInstance().setDefaultsAsync(defaultsToMap(defaults));
                    callbackContext.success();
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.logException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITREMOTECONFIG);
      }
    }

    private static Map<String, Object> defaultsToMap(JSONObject object) throws JSONException {
        final Map<String, Object> map = new HashMap<String, Object>();

        for (Iterator<String> keys = object.keys(); keys.hasNext(); ) {
            String key = keys.next();
            Object value = object.get(key);

            if (value instanceof Integer) {
                //setDefaults() should take Longs
                value = new Long((Integer) value);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                if (array.length() == 1 && array.get(0) instanceof String) {
                    //parse byte[] as Base64 String
                    value = Base64.decode(array.getString(0), Base64.DEFAULT);
                } else {
                    //parse byte[] as numeric array
                    byte[] bytes = new byte[array.length()];
                    for (int i = 0; i < array.length(); i++)
                        bytes[i] = (byte) array.getInt(i);
                    value = bytes;
                }
            }

            map.put(key, value);
        }
        return map;
    }

    //
    // Firebase Performace
    //

    private HashMap<String, Trace> traces = new HashMap<String, Trace>();

    private void startTrace(final CallbackContext callbackContext, final String name) {
        final FCMHMSPlugin self = this;
        if(FCMHMSPlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {

                      Trace myTrace = null;
                      if (self.traces.containsKey(name)) {
                          myTrace = self.traces.get(name);
                      }

                      if (myTrace == null) {
                          myTrace = FirebasePerformance.getInstance().newTrace(name);
                          myTrace.start();
                          self.traces.put(name, myTrace);
                      }

                      callbackContext.success();
                  } catch (Exception e) {
                      if(FCMHMSPlugin.crashlyticsInit()){
                        Crashlytics.logException(e);
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void incrementCounter(final CallbackContext callbackContext, final String name, final String counterNamed) {
        final FCMHMSPlugin self = this;
        if(FCMHMSPlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {

                      Trace myTrace = null;
                      if (self.traces.containsKey(name)) {
                          myTrace = self.traces.get(name);
                      }

                      if (myTrace != null && myTrace instanceof Trace) {
                          myTrace.incrementMetric(counterNamed, 1);
                          callbackContext.success();
                      } else {
                          callbackContext.error("Trace not found");
                      }
                  } catch (Exception e) {
                      if(FCMHMSPlugin.crashlyticsInit()){
                        Crashlytics.logException(e);
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void stopTrace(final CallbackContext callbackContext, final String name) {
        final FCMHMSPlugin self = this;
        if(FCMHMSPlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {

                      Trace myTrace = null;
                      if (self.traces.containsKey(name)) {
                          myTrace = self.traces.get(name);
                      }

                      if (myTrace != null && myTrace instanceof Trace) { //
                          myTrace.stop();
                          self.traces.remove(name);
                          callbackContext.success();
                      } else {
                          callbackContext.error("Trace not found");
                      }
                  } catch (Exception e) {
                      if(FCMHMSPlugin.crashlyticsInit()){
                        Crashlytics.logException(e);
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    private void setAnalyticsCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        final FCMHMSPlugin self = this;
        if(FCMHMSPlugin.analyticsInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {
                      mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
                      callbackContext.success();
                  } catch (Exception e) {
                      if(FCMHMSPlugin.crashlyticsInit()){
                        Crashlytics.log(e.getMessage());
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITANALYTICS);
        }
    }

    private void setPerformanceCollectionEnabled(final CallbackContext callbackContext, final boolean enabled) {
        final FCMHMSPlugin self = this;
        if(FCMHMSPlugin.performanceInit()){
          cordova.getThreadPool().execute(new Runnable() {
              public void run() {
                  try {
                      FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enabled);
                      callbackContext.success();
                  } catch (Exception e) {
                      if(FCMHMSPlugin.crashlyticsInit()){
                        Crashlytics.log(e.getMessage());
                      }
                      e.printStackTrace();
                      callbackContext.error(e.getMessage());
                  }
              }
          });
        } else {
          callbackContext.error(ERRORINITPERFORMANCE);
        }
    }

    public void clearAllNotifications(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    Context context = cordova.getActivity();
                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.cancelAll();
                    callbackContext.success();
                } catch (Exception e) {
                  if(FCMHMSPlugin.crashlyticsInit()){
                    Crashlytics.log(e.getMessage());
                  }
                }
            }
        });
    }
}
