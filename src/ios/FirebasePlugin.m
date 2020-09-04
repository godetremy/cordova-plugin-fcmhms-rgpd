#import "FirebasePlugin.h"
#import <Cordova/CDV.h>
#import "AppDelegate.h"
#import "Firebase.h"
@import FirebaseCrashlytics;
@import FirebaseInstanceID;
@import FirebaseMessaging;
@import FirebaseAnalytics;
@import FirebaseRemoteConfig;
@import FirebasePerformance;

#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
@import UserNotifications;
#endif

#ifndef NSFoundationVersionNumber_iOS_9_x_Max
#define NSFoundationVersionNumber_iOS_9_x_Max 1299
#endif

@implementation FCMHMSPlugin

@synthesize notificationCallbackId;
@synthesize tokenRefreshCallbackId;
@synthesize notificationStack;
@synthesize traces;

@synthesize firebaseInit;
@synthesize crashlyticsInit;
@synthesize analyticsInit;
@synthesize remoteconfigInit;
@synthesize performanceInit;

static NSInteger const kNotificationStackSize = 10;
static NSString * const ERRORINITFIREBASE = @"Firebase isn't initialised";
static NSString * const ERRORINITCRASHLYTICS = @"Crashlytics isn't initialised";
static NSString * const ERRORINITANALYTICS = @"Analytics isn't initialised";
static NSString * const ERRORINITREMOTECONFIG = @"RemoteConfig isn't initialised";
static NSString * const ERRORINITPERFORMANCE = @"Performance isn't initialised";
static FCMHMSPlugin *fcmhmsPlugin;

+ (FCMHMSPlugin *) fcmhmsPlugin {
    return fcmhmsPlugin;
}

- (void)pluginInitialize {
    NSLog(@"Starting Firebase plugin");
    fcmhmsPlugin = self;
    self.firebaseInit = NO;
    self.crashlyticsInit = NO;
    self.analyticsInit = NO;
    self.remoteconfigInit = NO;
    self.performanceInit = NO;
}

- (void)isGMS:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:true];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)isHMS:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:false];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)initFcmHms:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    if ([FIRApp defaultApp] == nil) {
      [FIRApp configure];
    }

    if ([FIRApp defaultApp] == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        self.firebaseInit = YES;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)initCrashlytics:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    if ([FIRApp defaultApp] == nil) {
      [FIRApp configure];
    }

    if ([FIRCrashlytics crashlytics] == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        self.crashlyticsInit = YES;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)initAnalytics:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    if ([FIRApp defaultApp] == nil) {
      [FIRApp configure];
    }

    if ([FIRAnalytics appInstanceID] == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        [FIRAnalytics setAnalyticsCollectionEnabled:YES];
        self.analyticsInit = YES;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)initPerformance:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    if ([FIRApp defaultApp] == nil) {
      [FIRApp configure];
    }

    if ([FIRPerformance sharedInstance] == nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    } else {
        [[FIRPerformance sharedInstance] setDataCollectionEnabled:YES];
        self.performanceInit = YES;
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)initRemoteConfig:(CDVInvokedUrlCommand *)command {
    [self activateFetched:command];
}

- (void)getId:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;

    FIRInstanceIDHandler handler = ^(NSString *_Nullable instID, NSError *_Nullable error) {
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:instID];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    };

    [[FIRInstanceID instanceID] getIDWithHandler:handler];
}

// DEPRECATED - alias of getToken
- (void)getInstanceId:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    if(self.firebaseInit){
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[[FIRMessaging messaging] FCMToken]];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITFIREBASE];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getToken:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    if(self.firebaseInit){
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[[FIRMessaging messaging] FCMToken]];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITFIREBASE];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)hasPermission:(CDVInvokedUrlCommand *)command {
    BOOL enabled = NO;
    UIApplication *application = [UIApplication sharedApplication];

    if ([[UIApplication sharedApplication] respondsToSelector:@selector(registerUserNotificationSettings:)]) {
        enabled = application.currentUserNotificationSettings.types != UIUserNotificationTypeNone;
    } else {
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
        enabled = application.enabledRemoteNotificationTypes != UIRemoteNotificationTypeNone;
#pragma GCC diagnostic pop
    }

    NSMutableDictionary* message = [NSMutableDictionary dictionaryWithCapacity:1];
    [message setObject:[NSNumber numberWithBool:enabled] forKey:@"isEnabled"];
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:command.callbackId];
}

- (void)grantPermission:(CDVInvokedUrlCommand *)command {
  if ([UNUserNotificationCenter class] != nil) {
    // iOS 10 or higher
    [UNUserNotificationCenter currentNotificationCenter].delegate = self;
    UNAuthorizationOptions authOptions = UNAuthorizationOptionAlert | UNAuthorizationOptionSound | UNAuthorizationOptionBadge;
    [[UNUserNotificationCenter currentNotificationCenter]
      requestAuthorizationWithOptions:authOptions
      completionHandler:^(BOOL granted, NSError * _Nullable error) {
        dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
          CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus: granted ? CDVCommandStatus_OK : CDVCommandStatus_ERROR];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          dispatch_async(dispatch_get_main_queue(), ^(void){
              [[UIApplication sharedApplication] registerForRemoteNotifications];
          });
        });
    }];
  } else {
    // iOS 10 notifications aren't available
    // fall back to iOS 8-9 notifications
    dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
      dispatch_async(dispatch_get_main_queue(), ^(void){
        UIUserNotificationType allNotificationTypes = (UIUserNotificationTypeSound | UIUserNotificationTypeAlert | UIUserNotificationTypeBadge);
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:allNotificationTypes categories:nil];
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        [[UIApplication sharedApplication] registerForRemoteNotifications];
      });
    });
  }
  return;
}

- (void)setBadgeNumber:(CDVInvokedUrlCommand *)command {
    int number = [[command.arguments objectAtIndex:0] intValue];

    [self.commandDelegate runInBackground:^{
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:number];

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getBadgeNumber:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        long badge = [[UIApplication sharedApplication] applicationIconBadgeNumber];

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble:badge];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)subscribe:(CDVInvokedUrlCommand *)command {
    NSString* topic = [NSString stringWithFormat:@"/topics/%@", [command.arguments objectAtIndex:0]];

    [[FIRMessaging messaging] subscribeToTopic: topic];

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unsubscribe:(CDVInvokedUrlCommand *)command {
    NSString* topic = [NSString stringWithFormat:@"/topics/%@", [command.arguments objectAtIndex:0]];

    [[FIRMessaging messaging] unsubscribeFromTopic: topic];

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)unregister:(CDVInvokedUrlCommand *)command {
    __block CDVPluginResult *pluginResult;
    if(self.firebaseInit){
      [[FIRInstanceID instanceID] deleteIDWithHandler:^void(NSError *_Nullable error) {
          if (error) {
              NSLog(@"Unable to delete instance");
          } else {
              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          }
      }];
    } else {
      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITFIREBASE];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)onNotificationOpen:(CDVInvokedUrlCommand *)command {
    self.notificationCallbackId = command.callbackId;

    if (self.notificationStack != nil && [self.notificationStack count]) {
        for (NSDictionary *userInfo in self.notificationStack) {
            [self sendNotification:userInfo];
        }
        [self.notificationStack removeAllObjects];
    }
}

- (void)onTokenRefresh:(CDVInvokedUrlCommand *)command {
    if(self.firebaseInit){
      self.tokenRefreshCallbackId = command.callbackId;
      NSString* currentToken = [[FIRMessaging messaging] FCMToken];

      if (currentToken != nil) {
          [self sendToken:currentToken];
      }
    } else {
      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITFIREBASE];
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)sendNotification:(NSDictionary *)userInfo {
    if (self.notificationCallbackId != nil) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:userInfo];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.notificationCallbackId];
    } else {
        if (!self.notificationStack) {
            self.notificationStack = [[NSMutableArray alloc] init];
        }

        // stack notifications until a callback has been registered
        [self.notificationStack addObject:userInfo];

        if ([self.notificationStack count] >= kNotificationStackSize) {
            [self.notificationStack removeLastObject];
        }
    }
}

- (void)sendToken:(NSString *)token {
    if (self.tokenRefreshCallbackId != nil) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:token];
        [pluginResult setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.tokenRefreshCallbackId];
    }
}

- (void)logEvent:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* name = [command.arguments objectAtIndex:0];

        NSString *description = NSLocalizedString([command argumentAtIndex:1 withDefault:@"No Message Provided"], nil);
        NSDictionary *parameters = @{ NSLocalizedDescriptionKey: description };

        if(self.analyticsInit){
          [FIRAnalytics logEventWithName:name parameters:parameters];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITANALYTICS];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)logError:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* errorMessage = [command.arguments objectAtIndex:0];
        if(self.crashlyticsInit){
          [[FIRCrashlytics crashlytics] logWithFormat:@"%@", errorMessage];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITCRASHLYTICS];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)setScreenName:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* name = [command.arguments objectAtIndex:0];

        if(self.analyticsInit){
          dispatch_async(dispatch_get_global_queue( DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            dispatch_async(dispatch_get_main_queue(), ^(void){
              [FIRAnalytics setScreenName:name screenClass:NULL];
            });
          });
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITANALYTICS];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)setUserId:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* id = [command.arguments objectAtIndex:0];

        if(self.analyticsInit){
          [FIRAnalytics setUserID:id];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITANALYTICS];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)setUserProperty:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* name = [command.arguments objectAtIndex:0];
        NSString* value = [command.arguments objectAtIndex:1];

        if(self.analyticsInit){
          [FIRAnalytics setUserPropertyString:value forName:name];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITANALYTICS];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)fetch:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        if(self.remoteconfigInit){
          FIRRemoteConfig* remoteConfig = [FIRRemoteConfig remoteConfig];

          if ([command.arguments count] > 0) {
              int expirationDuration = [[command.arguments objectAtIndex:0] intValue];

              [remoteConfig fetchWithExpirationDuration:expirationDuration completionHandler:^(FIRRemoteConfigFetchStatus status, NSError * _Nullable error) {
                  if (status == FIRRemoteConfigFetchStatusSuccess) {
                      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                  }
              }];
          } else {
              [remoteConfig fetchWithCompletionHandler:^(FIRRemoteConfigFetchStatus status, NSError * _Nullable error) {
                  if (status == FIRRemoteConfigFetchStatusSuccess) {
                      CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                  }
              }];
          }
        } else {
          CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITREMOTECONFIG];
          [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)activateFetched:(CDVInvokedUrlCommand *)command {
     [self.commandDelegate runInBackground:^{
        FIRRemoteConfig* remoteConfig = [FIRRemoteConfig remoteConfig];
         BOOL activated = [remoteConfig activateFetched];
         self.remoteconfigInit = activated;
         CDVPluginResult *pluginResult;

         if (activated) {
             pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
         } else {
             pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
         }

         [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
     }];
}

- (void)getValue:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        NSString* key = [command.arguments objectAtIndex:0];

        if(self.remoteconfigInit){
          FIRRemoteConfig* remoteConfig = [FIRRemoteConfig remoteConfig];
          NSString* value = remoteConfig[key].stringValue;
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:value];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITREMOTECONFIG];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

//
// Performace
//
- (void)startTrace:(CDVInvokedUrlCommand *)command {

    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        if(self.performanceInit){
          NSString* traceName = [command.arguments objectAtIndex:0];
          FIRTrace *trace = [self.traces objectForKey:traceName];

          if ( self.traces == nil) {
              self.traces = [NSMutableDictionary new];
          }

          if (trace == nil) {
              trace = [FIRPerformance startTraceWithName:traceName];
              [self.traces setObject:trace forKey:traceName ];

          }

          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITPERFORMANCE];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    }];
}

- (void)incrementCounter:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        if(self.performanceInit){
          NSString* traceName = [command.arguments objectAtIndex:0];
          NSString* counterNamed = [command.arguments objectAtIndex:1];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          FIRTrace *trace = (FIRTrace*)[self.traces objectForKey:traceName];

          if (trace != nil) {
              [trace incrementMetric:counterNamed byInt:1];
              [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          } else {
              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Trace not found"];
          }
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITPERFORMANCE];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    }];
}

- (void)stopTrace:(CDVInvokedUrlCommand *)command {
    [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        if(self.performanceInit){
          NSString* traceName = [command.arguments objectAtIndex:0];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
          FIRTrace *trace = [self.traces objectForKey:traceName];

          if (trace != nil) {
              [trace stop];
              [self.traces removeObjectForKey:traceName];
              [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
          } else {
              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Trace not found"];
          }
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITPERFORMANCE];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)setAnalyticsCollectionEnabled:(CDVInvokedUrlCommand *)command {
     [self.commandDelegate runInBackground:^{
        CDVPluginResult *pluginResult;
        BOOL enabled = [[command argumentAtIndex:0] boolValue];

        if(self.analyticsInit){
          [FIRAnalytics setAnalyticsCollectionEnabled:enabled];
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:ERRORINITANALYTICS];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
     }];
}

- (void)setPerformanceCollectionEnabled:(CDVInvokedUrlCommand *)command {
     [self.commandDelegate runInBackground:^{
         BOOL enabled = [[command argumentAtIndex:0] boolValue];

         [[FIRPerformance sharedInstance] setDataCollectionEnabled:enabled];

         CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

         [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
     }];
}

- (void)clearAllNotifications:(CDVInvokedUrlCommand *)command {
	[self.commandDelegate runInBackground:^{
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:1];
        [[UIApplication sharedApplication] setApplicationIconBadgeNumber:0];

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}
@end
