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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

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
    private static FirebaseCrashlytics Crashlytics;
    private static CordovaWebView appView;
    private static String TAG = "FCMHMSPlugin";
    private final String ERRORISGMS = "Cannot get isGMS";
    private final String ERRORISHMS = "Cannot get isHMS";
    private final String ERRORINIT = "FCM or GMS aren't initialised";
    private final String ERRORINITCRASHLYTICS = "Crashlytics isn't initialised";
    private final String ERRORINITANALYTICS = "Analytics isn't initialised";
    private final String ERRORINITREMOTECONFIG = "RemoteConfig isn't initialised";
    protected static final String KEY = "badge";

    private static boolean gmsAvailability = false;
    private static boolean hmsAvailability = false;
    private static boolean firebaseInit = false;
    private static boolean hmsInit = false;
    private static boolean crashlyticsInit = false;
    private static boolean analyticsInit = false;
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
        } else if (action.equals("setAnalyticsCollectionEnabled")) {
            this.setAnalyticsCollectionEnabled(callbackContext, args.getBoolean(0));
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
            Crashlytics.recordException(e);
          }
          callbackContext.error(ERRORINIT);
        }
    }

    private void initCrashlytics(final CallbackContext callbackContext) {
        final Context context = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "Initialising Crashlytics");
        try {
          Crashlytics = FirebaseCrashlytics.getInstance();
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
            Crashlytics.recordException(e);
          }
          callbackContext.error(ERRORINITANALYTICS);
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
                      cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            try {
                                FirebaseMessaging.getInstance().getToken().addOnCompleteListener( new OnCompleteListener<String>() {
                          @Override
                                  public void onComplete(Task<String> task){
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        FCMHMSPlugin.sendToken(task.getResult());
                                    }
                                  }
                                });
                            } catch (Exception e) {
                                handleExceptionWithContext(e, callbackContext);
                                }
                          }
                      });
                    } else {
                      callbackContext.error(ERRORINIT);
                    }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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

    private void getId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    if(FCMHMSPlugin.firebaseInit()){
                    FirebaseInstallations.getInstance().getId().addOnCompleteListener( new OnCompleteListener<String>() {
                          @Override
                      public void onComplete(Task<String> task){
                        if (task.isSuccessful() && task.getResult() != null) {
                            callbackContext.success(task.getResult());
                }
            }
        });
                  } else {
                    callbackContext.error(ERRORINIT);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.recordException(e);
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
                    cordova.getThreadPool().execute(new Runnable() {
                      public void run() {
                          try {
                              FirebaseMessaging.getInstance().getToken().addOnCompleteListener( new OnCompleteListener<String>() {
                        @Override
                                public void onComplete(Task<String> task){
                                  if (task.isSuccessful() && task.getResult() != null) {
                                      callbackContext.success(task.getResult());
                                  }
                                }
                              });
                          } catch (Exception e) {
                              handleExceptionWithContext(e, callbackContext);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                    FirebaseInstallations.getInstance().delete();
                    callbackContext.success();
                  } else {
                    callbackContext.error(ERRORINIT);
                  }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(new Exception(message));
                      callbackContext.success(1);
                    } else {
                      callbackContext.error(ERRORINITCRASHLYTICS);
                    }
                } catch (Exception e) {
                    if(FCMHMSPlugin.crashlyticsInit()){
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
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
                      Crashlytics.recordException(e);
                    }
                    callbackContext.error(e.getMessage());
                }
            }
        });
      } else {
        callbackContext.error(ERRORINITANALYTICS);
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
                        Crashlytics.recordException(e);
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
                    Crashlytics.recordException(e);
                  }
                }
            }
        });
    }

    protected static void handleExceptionWithContext(Exception e, CallbackContext context){
        String msg = e.toString();
        Log.e(TAG, msg);
        Crashlytics.recordException(e);
        context.error(msg);
    }
}
