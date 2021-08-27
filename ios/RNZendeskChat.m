
#import "RNZendeskChat.h"

#import <ZendeskCoreSDK/ZendeskCoreSDK.h>
#import <CommonUISDK/CommonUISDK.h>
#import <SDKConfigurations/SDKConfigurations.h>
#import <AnswerBotSDK/AnswerBotSDK.h>
#import <AnswerBotProvidersSDK/AnswerBotProvidersSDK.h>
#import <ChatSDK/ChatSDK.h>
#import <ChatProvidersSDK/ChatProvidersSDK.h>
#import <MessagingSDK/MessagingSDK.h>
#import <MessagingAPI/MessagingAPI.h>
#import <SupportSDK/SupportSDK.h>
#import <SupportProvidersSDK/SupportProvidersSDK.h>

#import <React/RCTConvert.h>

@implementation RCTConvert (ZDKChatFormFieldStatus)

RCT_ENUM_CONVERTER(ZDKFormFieldStatus,
                   (@{
                       @"REQUIRED": @(ZDKFormFieldStatusRequired),
                       @"OPTIONAL": @(ZDKFormFieldStatusOptional),
                       @"HIDDEN": @(ZDKFormFieldStatusHidden),
                    }),
                   ZDKFormFieldStatusOptional,
                   integerValue);

@end

@implementation RNZendeskChat

RCT_EXPORT_MODULE()

static NSString *const LOG_TAG = @"RNZendeskChat:";

- (UIColor *)colorFromHexString: (NSString *)hexString {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString: hexString];
    [scanner setScanLocation: 1]; // bypass '#' character
    [scanner scanHexInt: &rgbValue];
    
    return [UIColor colorWithRed: ((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}


- (ZDKChatFormConfiguration * _Nullable)getPreChatFormConfiguration:(NSDictionary*)preChatFormOptions {
    #define ParseFormFieldStatus(key)\
        ZDKFormFieldStatus key = [RCTConvert ZDKFormFieldStatus:preChatFormOptions[@"" #key]]
        ParseFormFieldStatus(name);
        ParseFormFieldStatus(email);
        ParseFormFieldStatus(phone);
        ParseFormFieldStatus(department);
    #undef ParseFormFieldStatus

    return [[ZDKChatFormConfiguration alloc] initWithName:name
                                             email:email
                                             phoneNumber:phone
                                             department:department];
}

- (ZDKChatConfiguration *) getChatConfiguration: (NSDictionary *)chatConfigurationOptions {
    ZDKChatConfiguration *chatConfiguration = [[ZDKChatConfiguration alloc] init];
        
    if(chatConfigurationOptions != Nil && chatConfigurationOptions != NULL) {
        if (chatConfigurationOptions[@"enableChatMenuActions"]) {
            BOOL isMenuActionsEnabled = [RCTConvert BOOL:chatConfigurationOptions[@"enableChatMenuActions"]];
            
            if(isMenuActionsEnabled) {
                if(chatConfigurationOptions[@"chatMenuActions"]) {
                    NSDictionary *chatMenuActions = chatConfigurationOptions[@"chatMenuActions"];
                    BOOL isEndChatMenuItemEnabled = YES;
                    BOOL isChatTranscriptMenuItemEnabled = YES;
                    
                    if(chatMenuActions[@"enableEndChatMenuItem"]) {
                        isEndChatMenuItemEnabled = [RCTConvert BOOL:chatMenuActions[@"enableEndChatMenuItem"]];
                    }
                    if(chatMenuActions[@"enableChatTranscriptMenuItem"]) {
                        isChatTranscriptMenuItemEnabled = [RCTConvert BOOL:chatMenuActions[@"enableChatTranscriptMenuItem"]];
                    }

                    BOOL enableOnlyEndChat = isEndChatMenuItemEnabled == YES && isChatTranscriptMenuItemEnabled == NO;
                    BOOL enableOnlyChatTranscript = isEndChatMenuItemEnabled == NO && isChatTranscriptMenuItemEnabled == YES;

                    if(enableOnlyEndChat) {
                        chatConfiguration.chatMenuActions = @[@(ZDKChatMenuActionEndChat)];
                    } else if(enableOnlyChatTranscript) {
                        chatConfiguration.chatMenuActions = @[@(ZDKChatMenuActionEmailTranscript)];
                    }
                }
            }
            else {
                chatConfiguration.chatMenuActions = @[];
            }
        }
        
        if(chatConfigurationOptions[@"enableAgentAvailability"]) {
            BOOL isAgentAvailabilityEnabled = [RCTConvert BOOL:chatConfigurationOptions[@"enableAgentAvailability"]];
            chatConfiguration.isAgentAvailabilityEnabled = isAgentAvailabilityEnabled;
        }
        
        if(chatConfigurationOptions[@"enableChatTranscriptPrompt"]) {
            BOOL isChatTranscriptPromptEnabled = [RCTConvert BOOL:chatConfigurationOptions[@"enableChatTranscriptPrompt"]];
            chatConfiguration.isChatTranscriptPromptEnabled = isChatTranscriptPromptEnabled;
        }
        
        if(chatConfigurationOptions[@"enableOfflineForm"]) {
            BOOL isOfflineFormEnabled = [RCTConvert BOOL:chatConfigurationOptions[@"enableOfflineForm"]];
            chatConfiguration.isOfflineFormEnabled = isOfflineFormEnabled;
        }
        
        if(chatConfigurationOptions[@"enablePreChatForm"]) {
            BOOL isPreChatFormEnabled = [RCTConvert BOOL:chatConfigurationOptions[@"enablePreChatForm"]];
            chatConfiguration.isPreChatFormEnabled = isPreChatFormEnabled;

            if(isPreChatFormEnabled == YES && chatConfigurationOptions[@"preChatFormOptions"]) {
                chatConfiguration.preChatFormConfiguration = [self getPreChatFormConfiguration: chatConfigurationOptions[@"preChatFormOptions"]];
            }
        }
    }
    
    return chatConfiguration;
}

- (ZDKHelpCenterUiConfiguration *) filterHelpCenterArticles: (NSDictionary*)filterOptions {
    ZDKHelpCenterUiConfiguration* hcConfig = [ZDKHelpCenterUiConfiguration new];
    if (filterOptions[@"type"] && [filterOptions[@"type"] isEqualToString:@"category"]) {
      NSLog(@"%@ filter articles by category with values: %@", LOG_TAG, filterOptions[@"values"]);
      [hcConfig setGroupType:ZDKHelpCenterOverviewGroupTypeCategory];
      [hcConfig setGroupIds:filterOptions[@"values"]];
    }
    else if (filterOptions[@"type"] && [filterOptions[@"type"] isEqualToString:@"section"]) {
      NSLog(@"%@ filter articles by section with values: %@", LOG_TAG, filterOptions[@"values"]);
      [hcConfig setGroupType:ZDKHelpCenterOverviewGroupTypeSection];
      [hcConfig setGroupIds:filterOptions[@"values"]];
    }
    else if (filterOptions[@"values"]) {
      NSLog(@"%@ filter articles by label with values: %@", LOG_TAG, filterOptions[@"values"]);
      [hcConfig setLabels:filterOptions[@"values"]];
    }
    
    return hcConfig;
}

- (void) chatCloseClicked {
    UIViewController *topController = [UIApplication sharedApplication].keyWindow.rootViewController;
    while (topController.presentedViewController) {
        topController = topController.presentedViewController;
    }
    [topController dismissViewControllerAnimated: TRUE completion: NULL];
}

- (void) startChatFunction: (NSDictionary *)options {
    ZDKMessagingConfiguration *messagingConfiguration = [ZDKMessagingConfiguration new];
    
    NSString *botName = @"Answer Bot";

    if (options[@"botName"]) {
      botName = options[@"botName"];
    }
    
    messagingConfiguration.name = botName;

    NSError *error = nil;
    NSMutableArray *engines = [[NSMutableArray alloc] init];
    
    if (options[@"chatOnly"]) {
      engines = @[
        (id <ZDKEngine>) [ZDKChatEngine engineAndReturnError: &error]
      ];
    }
    else {
      engines = @[
        (id <ZDKEngine>) [ZDKAnswerBotEngine engineAndReturnError: &error],
        (id <ZDKEngine>) [ZDKChatEngine engineAndReturnError: &error],
        (id <ZDKEngine>) [ZDKSupportEngine engineAndReturnError: &error]
      ];
    }

    ZDKChatConfiguration *chatConfiguration = [self getChatConfiguration: options[@"chatConfigurationOptions"]];
    
    UIViewController *chatViewController = [[ZDKMessaging instance] buildUIWithEngines: engines
                                                                    configs: @[messagingConfiguration, chatConfiguration]
                                                                    error: &error];
    if (error) {
      NSLog(@"%@ Error occured %@", LOG_TAG, error);
    }
    
    UIBarButtonItem *buttonClose = [[UIBarButtonItem alloc] initWithBarButtonSystemItem: UIBarButtonSystemItemStop
                                                            target: self
                                                            action: @selector(chatCloseClicked)];

    if (options[@"closeButtonText"]) {
        buttonClose = [[UIBarButtonItem alloc] initWithTitle: options[@"closeButtonText"]
                                               style: UIBarButtonItemStylePlain
                                               target: self
                                               action: @selector(chatCloseClicked)];
    }
    
    chatViewController.navigationItem.leftBarButtonItem = buttonClose;

    UIViewController *topViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
    
    while (topViewController.presentedViewController) {
        topViewController = topViewController.presentedViewController;
    }

    UINavigationController *navigationViewController = [[UINavigationController alloc] initWithRootViewController: chatViewController];
    
    [topViewController presentViewController: navigationViewController animated: YES completion: nil];
    NSLog(@"%@ Started Chat with success!", LOG_TAG);
}

- (void) showHelpCenterFunction: (NSDictionary *)options {
    NSError *error = nil;
    NSArray *engines = @[];
    
    if (options[@"withChat"]) {
        engines = @[
          (id <ZDKEngine>) [ZDKChatEngine engineAndReturnError: &error]
        ];
    }
    
    ZDKHelpCenterUiConfiguration* helpCenterUiConfig = [ZDKHelpCenterUiConfiguration new];
    ZDKArticleUiConfiguration* articleUiConfig = [ZDKArticleUiConfiguration new];
    
    if(options[@"articlesFilter"] && options[@"articlesFilter"] != Nil && options[@"articlesFilter"] != NULL) {
      helpCenterUiConfig = [self filterHelpCenterArticles: options[@"articlesFilter"]];
    }

    if (options[@"disableTicketCreation"]) {
      helpCenterUiConfig.showContactOptions = NO;
      articleUiConfig.showContactOptions = NO;
    }
    
    helpCenterUiConfig.objcEngines = engines;
    articleUiConfig.objcEngines = engines;
    
    UIViewController *topViewController = [UIApplication sharedApplication].keyWindow.rootViewController;
    
    while (topViewController.presentedViewController) {
        topViewController = topViewController.presentedViewController;
    }
    
    UIViewController* viewController = [ZDKHelpCenterUi buildHelpCenterOverviewUiWithConfigs: @[helpCenterUiConfig, articleUiConfig]];

    UINavigationController *navigationViewController = [[UINavigationController alloc] initWithRootViewController: viewController];
    
    [topViewController presentViewController: navigationViewController animated: YES completion: nil];
    NSLog(@"%@ Showed Help Center with success!", LOG_TAG);
}

RCT_EXPORT_METHOD(init: (NSDictionary *)options) {
  NSLog(@"%@ accountKey: %@, appId: %@, clientId: %@, supportUrl: %@", LOG_TAG, options[@"accountKey"], options[@"appId"], options[@"clientId"], options[@"supportUrl"]);
    
  [ZDKZendesk initializeWithAppId: options[@"appId"]
              clientId: options[@"clientId"]
              zendeskUrl: options[@"supportUrl"]];
  [ZDKSupport initializeWithZendesk: [ZDKZendesk instance]];
  [ZDKAnswerBot initializeWithZendesk: [ZDKZendesk instance] support: [ZDKSupport instance]];
  [ZDKChat initializeWithAccountKey: options[@"accountKey"] queue: dispatch_get_main_queue()];
  NSLog(@"%@ Init all Unified SDKs with success!", LOG_TAG);
}

RCT_EXPORT_METHOD(initChat: (NSString *)accountKey) {
  [ZDKChat initializeWithAccountKey: accountKey queue: dispatch_get_main_queue()];
  NSLog(@"%@ Init Chat SDK with success!", LOG_TAG);
}

RCT_EXPORT_METHOD(resetUserIdentity) {
    [[ZDKChat instance] resetIdentity:^{
        // Identity was reset
        NSLog(@"%@ User identity was reset with success.", LOG_TAG);
    }];
}

RCT_EXPORT_METHOD(setUserIdentity: (NSDictionary *)userIdentityInfo) {
  if (userIdentityInfo[@"token"]) {
    id<ZDKObjCIdentity> userIdentity = [[ZDKObjCJwt alloc] initWithToken: userIdentityInfo[@"token"]];
    [[ZDKZendesk instance] setIdentity: userIdentity];
    NSLog(@"%@ User Identity set with token: %@", LOG_TAG, userIdentityInfo[@"token"]);
  }
  else {
    id<ZDKObjCIdentity> userIdentity = [[ZDKObjCAnonymous alloc] initWithName: userIdentityInfo[@"name"] email: userIdentityInfo[@"email"]];
    [[ZDKZendesk instance] setIdentity: userIdentity];
    NSLog(@"%@ User Identity set with name: %@ and email: %@", LOG_TAG, userIdentityInfo[@"name"], userIdentityInfo[@"email"]);
  }
}

RCT_EXPORT_METHOD(setVisitorInfo: (NSDictionary *)visitorInfo) {
  ZDKChatAPIConfiguration *config = [[ZDKChatAPIConfiguration alloc] init];
    
  if (visitorInfo[@"department"] && visitorInfo[@"department"] != Nil && visitorInfo[@"department"] != NULL) {
    config.department = visitorInfo[@"department"];
  }
    
  if (visitorInfo[@"tags"]) {
    config.tags = visitorInfo[@"tags"];
  }
    
  config.visitorInfo = [[ZDKVisitorInfo alloc] initWithName: visitorInfo[@"name"]
                                               email: visitorInfo[@"email"]
                                               phoneNumber: visitorInfo[@"phone"]];
  ZDKChat.instance.configuration = config;
  
  NSLog(@"%@ Visitor Info set with success: %@", LOG_TAG, visitorInfo);
}

RCT_EXPORT_METHOD(setPrimaryColor: (NSString *)color) {
  [ZDKCommonTheme currentTheme].primaryColor = [self colorFromHexString: color];
}

RCT_EXPORT_METHOD(setNotificationToken: (NSData *)token) {
  dispatch_sync(dispatch_get_main_queue(), ^{
    [ZDKChat registerPushToken: token];
  });
}

RCT_EXPORT_METHOD(startChat: (NSDictionary *)options) {
  if(options[@"userIdentity"]) {
    [self setUserIdentity: options[@"userIdentity"]];
  }
    
  if(options[@"visitorInfo"]) {
    [self setVisitorInfo: options[@"visitorInfo"]];
  }

  dispatch_sync(dispatch_get_main_queue(), ^{
    [self startChatFunction: options];
  });
}

RCT_EXPORT_METHOD(showHelpCenter: (NSDictionary *)options) {
  if(options[@"userIdentity"]) {
    [self setUserIdentity: options[@"userIdentity"]];
  }
    
  if(options[@"visitorInfo"]) {
    [self setVisitorInfo: options[@"visitorInfo"]];
  }
    
  dispatch_sync(dispatch_get_main_queue(), ^{
    [self showHelpCenterFunction: options];
  });
}

@end
