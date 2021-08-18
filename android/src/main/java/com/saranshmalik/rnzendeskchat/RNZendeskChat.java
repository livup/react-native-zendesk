
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
import zendesk.support.Support;
import zendesk.support.SupportEngine;
import zendesk.support.guide.HelpCenterActivity;
import zendesk.support.guide.ViewArticleActivity;
import zendesk.messaging.MessagingActivity;
import zendesk.messaging.MessagingConfiguration;

public class RNZendeskChat extends ReactContextBaseJavaModule {

    private ReactContext appContext;
    private static final String TAG = "RNZendeskChat";

    public RNZendeskChat(ReactApplicationContext reactContext) {
        super(reactContext);
        appContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNZendeskChat";
    }

    private ChatConfiguration getChatConfiguration() {
        return ChatConfiguration.builder()
                .withAgentAvailabilityEnabled(true)
                .withOfflineFormEnabled(true)
                .build();
    }

    private void setChatInfo(ReadableMap options) {
        Providers providers = Chat.INSTANCE.providers();

        if (providers == null) {
            Log.d(TAG, "Can't set chat info, instance providers is null");
            return;
        }

        ChatProvider chatProvider = providers.chatProvider();

        if (chatProvider == null) {
            Log.d(TAG, "Chat provider is null");
            return;
        }

        if (options.hasKey("department")) {
            String department = options.getString("department");
            chatProvider.setDepartment(department, null);
        }
    }

    @ReactMethod
    public void init(ReadableMap options) {
        String chatAccountKey = options.getString("key");
        String appId = options.getString("appId");
        String oauthClientId = options.getString("clientId");
        String supportUrl = options.getString("url");

        Context context = appContext;

        Zendesk.INSTANCE.init(context, supportUrl, appId, oauthClientId);
        Support.INSTANCE.init(Zendesk.INSTANCE);
        AnswerBot.INSTANCE.init(Zendesk.INSTANCE, Support.INSTANCE);
        Chat.INSTANCE.init(context, chatAccountKey);
    }

    @ReactMethod
    public void initChat(String chatAccountKey) {
        Context context = appContext;
        Chat.INSTANCE.init(context, chatAccountKey);
    }

    @ReactMethod
    public void setVisitorInfo(ReadableMap options) {
        Providers providers = Chat.INSTANCE.providers();

        if (providers == null) {
            Log.d(TAG, "Can't set visitor info, instance providers is null");
            return;
        }

        ProfileProvider profileProvider = providers.profileProvider();

        if (profileProvider == null) {
            Log.d(TAG, "Can't set visitor info, Profile provider is null");
            return;
        }

        VisitorInfo.Builder visitorInfoBuilder = VisitorInfo.builder();

        if (options.hasKey("name")) {
            String name = options.getString("name");
            visitorInfoBuilder = visitorInfoBuilder.withName(name);
        }
        if (options.hasKey("email")) {
            String email = options.getString("email");
            visitorInfoBuilder = visitorInfoBuilder.withEmail(email);
        }
        if (options.hasKey("phone")) {
            String phone = options.getString("phone");
            visitorInfoBuilder = visitorInfoBuilder.withPhoneNumber(phone);
        }

        VisitorInfo visitorInfo = visitorInfoBuilder.build();
        profileProvider.setVisitorInfo(visitorInfo, null);

        if(options.hasKey("tags")) {
            ReadableArray readableTags = options.getArray("tags");
            List<String> tags = new ArrayList<String>();
            for (int i = 0; i < readableTags.size(); i++) {
                tags.add(readableTags.getString(i));
            }
            profileProvider.addVisitorTags(tags, null);
        }
    }

    @ReactMethod
    public void setUserIdentity(ReadableMap options) {
        if (options.hasKey("token")) {
            String token = options.getString("token");
            Identity identity = new JwtIdentity(token);
            Zendesk.INSTANCE.setIdentity(identity);
        } else {
            String name = options.getString("name");
            String email = options.getString("email");
            Identity identity = new AnonymousIdentity.Builder()
                    .withNameIdentifier(name)
                    .withEmailIdentifier(email)
                    .build();
            Zendesk.INSTANCE.setIdentity(identity);
        }
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
    public void resetUserIdentity() {
        Chat.INSTANCE.resetIdentity(new CompletionCallback<Void>() {
            @Override
            public void onCompleted(Void result) {
                // Identity was reset
                Log.d(TAG, "User identity reset with success.");
            }
        });
    }

    @ReactMethod
    public void startChat(ReadableMap options) {
        setUserIdentity(options);
        setVisitorInfo(options);
        setChatInfo(options);

        Activity activity = getCurrentActivity();

        MessagingConfiguration.Builder messagingActivityBuilder = MessagingActivity.builder();

        if(options.hasKey("toolbarTitle")) {
            String toolbarTitle = options.getString("toolbarTitle");
            messagingActivityBuilder
                    .withToolbarTitle(toolbarTitle);
        }

        if(options.hasKey("botName")) {
            String botName = options.getString("botName");
            messagingActivityBuilder
                    .withBotLabelString(botName);
        }

        if (options.hasKey("chatOnly")) {
            messagingActivityBuilder.withEngines(ChatEngine.engine(), SupportEngine.engine());
        } else {
            messagingActivityBuilder.withEngines(AnswerBotEngine.engine(), ChatEngine.engine(), SupportEngine.engine());
        }

        messagingActivityBuilder.show(activity, getChatConfiguration());
    }

    @ReactMethod
    public void showHelpCenter(ReadableMap options) {
        setUserIdentity(options);

        Activity activity = getCurrentActivity();

        if (options.hasKey("withChat")) {
            HelpCenterActivity.builder()
                    .withEngines(ChatEngine.engine())
                    .show(activity);
        } else if (options.hasKey("disableTicketCreation")) {
            HelpCenterActivity.builder()
                    .withContactUsButtonVisible(false)
                    .withShowConversationsMenuButton(false)
                    .show(activity, ViewArticleActivity.builder()
                            .withContactUsButtonVisible(false)
                            .config());
        } else {
            HelpCenterActivity.builder()
                    .show(activity);
        }
    }
}
