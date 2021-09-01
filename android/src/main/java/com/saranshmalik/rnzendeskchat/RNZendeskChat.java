
package com.saranshmalik.rnzendeskchat;

import android.app.Activity;
import android.content.Context;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;

import zendesk.chat.ChatMenuAction;
import zendesk.chat.ChatSessionStatus;
import zendesk.chat.ChatState;
import zendesk.chat.ObservationScope;
import zendesk.chat.Observer;
import zendesk.chat.PreChatFormFieldStatus;
import zendesk.configurations.Configuration;
import zendesk.core.Zendesk;
import zendesk.core.JwtIdentity;
import zendesk.core.AnonymousIdentity;
import zendesk.core.Identity;
import zendesk.answerbot.AnswerBot;
import zendesk.answerbot.AnswerBotEngine;
import zendesk.chat.Chat;
import zendesk.chat.ChatConfiguration;
import zendesk.chat.ChatEngine;
import zendesk.chat.ChatProvider;
import zendesk.chat.ProfileProvider;
import zendesk.chat.PushNotificationsProvider;
import zendesk.chat.Providers;
import zendesk.chat.VisitorInfo;
import zendesk.chat.CompletionCallback;
import zendesk.messaging.MessagingActivity;
import zendesk.messaging.MessagingConfiguration;
import zendesk.support.Support;
import zendesk.support.SupportEngine;
import zendesk.support.guide.HelpCenterActivity;
import zendesk.support.guide.HelpCenterConfiguration;
import zendesk.support.guide.ViewArticleActivity;

public class RNZendeskChat extends ReactContextBaseJavaModule {

    private ReactContext appContext;
    private static final String TAG = "RNZendeskChat";
    private boolean isVisitorInfoSet = false;
    private VisitorInfo builtVisitorInfo = null;

    public RNZendeskChat(ReactApplicationContext reactContext) {
        super(reactContext);
        appContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNZendeskChat";
    }

    private void setupChatObserver(){
        isVisitorInfoSet = false;
        final ObservationScope observationScope = new ObservationScope();
        final ProfileProvider profileProvider =  Chat.INSTANCE.providers().profileProvider();

        Chat.INSTANCE.providers().chatProvider().observeChatState(observationScope, new Observer<ChatState>() {
            @Override
            public void update(ChatState chatState) {
                ChatSessionStatus chatStatus = chatState.getChatSessionStatus();
                // Status achieved after the PreChatForm is completed
                if (chatStatus == ChatSessionStatus.STARTED) {
                    // Update the information MID chat here. All info but Department can be updated
                    if (!isVisitorInfoSet && builtVisitorInfo != null) {
                        profileProvider.setVisitorInfo(builtVisitorInfo, null);
                        isVisitorInfoSet = true;
                        Log.d(TAG, "[observerSetup] - Updated VisitorInfo!");
                    }
                }
            }
        });
    }

    private ChatConfiguration getChatConfiguration(ReadableMap chatConfigurationOptions) {

        ChatConfiguration.Builder chatConfigurationBuilder = ChatConfiguration.builder();

        if(chatConfigurationOptions != null) {
            if(chatConfigurationOptions.hasKey("enableChatMenuActions")) {
                boolean isMenuActionsEnabled = chatConfigurationOptions.getBoolean("enableChatMenuActions");

                if(isMenuActionsEnabled) {
                    if(chatConfigurationOptions.hasKey("chatMenuActions")) {
                        ReadableMap chatMenuActions = chatConfigurationOptions.getMap("chatMenuActions");
                        boolean isEndChatMenuItemEnabled = true;
                        boolean isChatTranscriptMenuItemEnabled = true;

                        if(chatMenuActions.hasKey("enableEndChatMenuItem")) {
                            isEndChatMenuItemEnabled = chatMenuActions.getBoolean("enableEndChatMenuItem");
                        }
                        if(chatMenuActions.hasKey("enableChatTranscriptMenuItem")) {
                            isChatTranscriptMenuItemEnabled = chatMenuActions.getBoolean("enableChatTranscriptMenuItem");
                        }

                        boolean enableOnlyEndChat = isEndChatMenuItemEnabled == true && isChatTranscriptMenuItemEnabled ==  false;
                        boolean enableOnlyChatTranscript = isEndChatMenuItemEnabled == false && isChatTranscriptMenuItemEnabled ==  true;

                        if(enableOnlyEndChat) {
                            chatConfigurationBuilder.withChatMenuActions(ChatMenuAction.END_CHAT);
                        } else if(enableOnlyChatTranscript) {
                            chatConfigurationBuilder.withChatMenuActions(ChatMenuAction.CHAT_TRANSCRIPT);
                        }
                    }
                } else {
                    chatConfigurationBuilder.withChatMenuActions();
                }
            }

            if(chatConfigurationOptions.hasKey("enableAgentAvailability")) {
                boolean isAgentAvailabilityEnabled = chatConfigurationOptions.getBoolean("enableAgentAvailability");
                chatConfigurationBuilder.withAgentAvailabilityEnabled(isAgentAvailabilityEnabled);
            }

            if(chatConfigurationOptions.hasKey("enableChatTranscriptPrompt")) {
                boolean isChatTranscriptPromptEnabled = chatConfigurationOptions.getBoolean("enableChatTranscriptPrompt");
                chatConfigurationBuilder.withTranscriptEnabled(isChatTranscriptPromptEnabled);
            }

            if(chatConfigurationOptions.hasKey("enableOfflineForm")) {
                boolean isOfflineFormEnabled = chatConfigurationOptions.getBoolean("enableOfflineForm");
                chatConfigurationBuilder.withOfflineFormEnabled(isOfflineFormEnabled);
            }

            if(chatConfigurationOptions.hasKey("enablePreChatForm")) {
                boolean isPreChatFormEnabled = chatConfigurationOptions.getBoolean("enablePreChatForm");
                chatConfigurationBuilder.withPreChatFormEnabled(isPreChatFormEnabled);

                if(isPreChatFormEnabled == true && chatConfigurationOptions.hasKey("preChatFormOptions")) {
                    ReadableMap preChatFormOptions = chatConfigurationOptions.getMap("preChatFormOptions");

                    if(preChatFormOptions.hasKey("name")) {
                        chatConfigurationBuilder.withNameFieldStatus(PreChatFormFieldStatus.valueOf(preChatFormOptions.getString("name")));
                    }
                    if(preChatFormOptions.hasKey("email")) {
                        chatConfigurationBuilder.withEmailFieldStatus(PreChatFormFieldStatus.valueOf(preChatFormOptions.getString("email")));
                    }
                    if(preChatFormOptions.hasKey("phone")) {
                        chatConfigurationBuilder.withPhoneFieldStatus(PreChatFormFieldStatus.valueOf(preChatFormOptions.getString("phone")));
                    }
                    if(preChatFormOptions.hasKey("department")) {
                        chatConfigurationBuilder.withDepartmentFieldStatus(PreChatFormFieldStatus.valueOf(preChatFormOptions.getString("department")));
                    }
                }
            }
        }

        return chatConfigurationBuilder.build();
    }

    private List getArticleFilteredValues (ReadableMap articlesFilter, String filterType) {
        ReadableArray filterValues = articlesFilter.getArray("values");
        boolean isLongValuesType = filterType.equals("category") || filterType.equals("section");

        List values = new ArrayList<>();
        for (int i = 0; i < filterValues.size(); i++) {
            if(isLongValuesType) {
                values.add(Long.valueOf(filterValues.getString(i)));
            } else {
                values.add(filterValues.getString(i));
            }
        }

        return values;
    }

    @ReactMethod
    public void init(ReadableMap options) {
        String accountKey = options.getString("accountKey");
        String appId = options.getString("appId");
        String clientId = options.getString("clientId");
        String supportUrl = options.getString("supportUrl");

        Log.d(TAG, "accountKey: " + accountKey + ", appId: " + appId + ", clientId: " + clientId + ", supportUrl: " + supportUrl);

        Context context = appContext;

        Zendesk.INSTANCE.init(context, supportUrl, appId, clientId);
        Support.INSTANCE.init(Zendesk.INSTANCE);
        AnswerBot.INSTANCE.init(Zendesk.INSTANCE, Support.INSTANCE);
        Chat.INSTANCE.init(context, accountKey);
        Log.d(TAG, "Init all Unified SDKs with success!");
    }

    @ReactMethod
    public void initChat(String accountKey) {
        Context context = appContext;
        Chat.INSTANCE.init(context, accountKey);
        Log.d(TAG, "Init Chat SDK with success!");
    }

    @ReactMethod
    public void resetUserIdentity() {
        Chat.INSTANCE.resetIdentity(new CompletionCallback<Void>()  {
            @Override
            public void onCompleted(Void result) {
                // Identity was reset
                Log.d(TAG, "User identity was reset with success.");
            }
        });
    }

    @ReactMethod
    public void setUserIdentity(ReadableMap userIdentityInfo) {
        if (userIdentityInfo.hasKey("token")) {
            String token = userIdentityInfo.getString("token");
            Identity identity = new JwtIdentity(token);

            Zendesk.INSTANCE.setIdentity(identity);
            Log.d(TAG, "User Identity set with token: " + token);
        } else {
            String name = userIdentityInfo.getString("name");
            String email = userIdentityInfo.getString("email");
            Identity identity = new AnonymousIdentity.Builder()
                    .withNameIdentifier(name)
                    .withEmailIdentifier(email)
                    .build();

            Zendesk.INSTANCE.setIdentity(identity);
            Log.d(TAG, "User Identity set with name: " + name + " and email: " + email);
        }
    }

    @ReactMethod
    public void setVisitorInfo(ReadableMap visitorInfo) {
        Providers providers = Chat.INSTANCE.providers();
        if (providers == null) {
            Log.d(TAG, "Can't set visitor info, Chat Instance Providers is null");
            return;
        }

        ChatProvider chatProvider = providers.chatProvider();
        if (chatProvider != null) {
            if (visitorInfo.hasKey("department") && visitorInfo.getString("department") != null) {
                String department = visitorInfo.getString("department");
                chatProvider.setDepartment(department, null);
            }
        } else {
            Log.d(TAG, "Can't set department because Chat provider is null");
        }

        VisitorInfo.Builder visitorInfoBuilder = VisitorInfo.builder();

        if (visitorInfo.hasKey("name")) {
            String name = visitorInfo.getString("name");
            visitorInfoBuilder.withName(name);
        }
        if (visitorInfo.hasKey("email")) {
            String email = visitorInfo.getString("email");
            visitorInfoBuilder.withEmail(email);
        }
        if (visitorInfo.hasKey("phone")) {
            String phone = visitorInfo.getString("phone");
            visitorInfoBuilder.withPhoneNumber(phone);
        }

        builtVisitorInfo = visitorInfoBuilder.build();

        ProfileProvider profileProvider = providers.profileProvider();
        if (profileProvider == null) {
            Log.d(TAG, "Can't set visitor info, because Profile provider is null");
            return;
        }

        if(visitorInfo.hasKey("tags")) {
            ReadableArray readableTags = visitorInfo.getArray("tags");
            List<String> tags = new ArrayList<String>();

            for (int i = 0; i < readableTags.size(); i++) {
                tags.add(readableTags.getString(i));
            }

            profileProvider.addVisitorTags(tags, null);
        }

        Log.d(TAG, "Visitor Info set with success: " + visitorInfo);
    }

    @ReactMethod
    public void setNotificationToken(String token) {
        Providers providers = Chat.INSTANCE.providers();

        if (providers == null) {
            Log.d(TAG, "Can't set notification token, instance providers is null");
            return;
        }

        PushNotificationsProvider pushProvider = providers.pushNotificationsProvider();

        if (pushProvider != null) {
            pushProvider.registerPushToken(token);
        }
    }

    @ReactMethod
    public void startChat(ReadableMap options) {
        if(options.hasKey("userIdentity")) {
            setUserIdentity(options.getMap("userIdentity"));
        }

        if(options.hasKey("visitorInfo")) {
            setVisitorInfo(options.getMap("visitorInfo"));
        }

        Activity activity = getCurrentActivity();
        MessagingConfiguration.Builder messagingActivityBuilder = MessagingActivity.builder();

        if(options.hasKey("toolbarTitle")) {
            String toolbarTitle = options.getString("toolbarTitle");
            messagingActivityBuilder.withToolbarTitle(toolbarTitle);
        }

        String botName = "Answer Bot";

        if(options.hasKey("botName")) {
            botName = options.getString("botName");
        }

        messagingActivityBuilder.withBotLabelString(botName);

        if (options.hasKey("chatOnly")) {
            messagingActivityBuilder.withEngines(ChatEngine.engine());
        } else {
            messagingActivityBuilder.withEngines(AnswerBotEngine.engine(), ChatEngine.engine(), SupportEngine.engine());
        }

        ReadableMap chatConfigOptions = null;

        if(options.hasKey("chatConfigurationOptions")) {
            chatConfigOptions = options.getMap("chatConfigurationOptions");
        }

        setupChatObserver();

        messagingActivityBuilder.show(activity, getChatConfiguration(chatConfigOptions));
        Log.d(TAG, "Started Chat with success!");
    }

    @ReactMethod
    public void showHelpCenter(ReadableMap options) {
        if(options.hasKey("userIdentity")) {
            setUserIdentity(options.getMap("userIdentity"));
        }

        if(options.hasKey("visitorInfo")) {
            setVisitorInfo(options.getMap("visitorInfo"));
        }

        Activity activity = getCurrentActivity();
        HelpCenterConfiguration.Builder helpCenterActivityBuilder = HelpCenterActivity.builder();

        if (options.hasKey("articlesFilter") && options.getMap("articlesFilter") != null) {
            try {
                ReadableMap articlesFilter = options.getMap("articlesFilter");
                String filterType = articlesFilter.getString("type");
                List filterValuesList = getArticleFilteredValues(articlesFilter, filterType);

                if(filterType.equals("category")) {
                    Log.d(TAG, "filter articles by category with values: " + filterValuesList);
                    helpCenterActivityBuilder.withArticlesForCategoryIds(filterValuesList);
                }
                else if(filterType == "section") {
                    Log.d(TAG, "filter articles by section with values: " + filterValuesList);
                    helpCenterActivityBuilder.withArticlesForSectionIds(filterValuesList);
                }
                else {
                    Log.d(TAG, "filter articles by label with values: " + filterValuesList);
                    helpCenterActivityBuilder.withLabelNames(filterValuesList);
                }
            } catch(Exception ex) {
                Log.d(TAG, "An Error occurred when applying article filters: " + ex.getMessage());
            }
        }

        if (options.hasKey("withChat")) {
            helpCenterActivityBuilder
                    .withEngines(ChatEngine.engine())
                    .show(activity);
        } 
        else if (options.hasKey("disableTicketCreation")) {
            Configuration articleConfiguration = ViewArticleActivity.builder().withContactUsButtonVisible(false).config();

            helpCenterActivityBuilder
                    .withContactUsButtonVisible(false)
                    .withShowConversationsMenuButton(false)
                    .show(activity, articleConfiguration);
        } 
        else {
            helpCenterActivityBuilder
                    .show(activity);
        }

        Log.d(TAG, "Showed Help Center with success!");
    }
}
