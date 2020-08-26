#import "AppDelegate.h"

#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
@import UserNotifications;
#endif

@interface AppDelegate (FCMHMSPlugin)
@property (nonatomic, strong, nonnull) NSNumber *applicationInBackground;
#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0
@property (NS_NONATOMIC_IOSONLY, nullable, weak) id <UNUserNotificationCenterDelegate> delegate;
#endif
@end
