#import <Cordova/CDV.h>
#import "AppDelegate.h"

@interface FCMHMSPlugin : CDVPlugin
+ (FCMHMSPlugin *) fcmhmsPlugin;
- (void)isGMS:(CDVInvokedUrlCommand*)command;
- (void)isHMS:(CDVInvokedUrlCommand*)command;
- (void)initFcmHms:(CDVInvokedUrlCommand*)command;
- (void)getToken:(CDVInvokedUrlCommand*)command;
- (void)grantPermission:(CDVInvokedUrlCommand*)command;
- (void)hasPermission:(CDVInvokedUrlCommand*)command;
- (void)setBadgeNumber:(CDVInvokedUrlCommand*)command;
- (void)getBadgeNumber:(CDVInvokedUrlCommand*)command;
- (void)subscribe:(CDVInvokedUrlCommand*)command;
- (void)unsubscribe:(CDVInvokedUrlCommand*)command;
- (void)unregister:(CDVInvokedUrlCommand*)command;
- (void)onNotificationOpen:(CDVInvokedUrlCommand*)command;
- (void)onTokenRefresh:(CDVInvokedUrlCommand*)command;
- (void)sendNotification:(NSDictionary*)userInfo;
- (void)sendToken:(NSString*)token;
- (void)clearAllNotifications:(CDVInvokedUrlCommand *)command;
@property (nonatomic, copy) NSString *notificationCallbackId;
@property (nonatomic, copy) NSString *tokenRefreshCallbackId;
@property (nonatomic, retain) NSMutableArray *notificationStack;
@property (nonatomic, readwrite) NSMutableDictionary* traces;
@property (atomic, assign) BOOL firebaseInit;

@end
