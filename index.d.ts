declare module 'react-native-zendesk-v2' {

  type PreChatFormFieldStatusType = 'REQUIRED' | 'OPTIONAL' | 'HIDDEN'
  type ArticlesFilterType = 'category' | 'section' | 'label'
  
  /**
  * Unified SDK initialization options
  */
  interface ISDKInitOptions {
    /** 
    * Account key of your Zendesk account 
    */
    accountKey: string,
    /**
    * AppId of your Zendesk mobile SDK 
    */
    appId: string,
    /**
    * ClientId of your Zendesk mobile SDK 
    */
    clientId: string,
    /**
    * Support URL of your Zendesk mobile SDK (should end with /)
    */
    supportUrl: string,
  }

  interface IUserIdentity {
    /*
    * Creates a JWT identity that will be used during identification of a user. 
    * The user identifier will typically be used by the remote system to identify the user there and 
    * pass back a token which will be used to identify that user in the Zendesk instance.
    */
    jwtToken?: string,
    /**
    * User name 
    */
    name: string
    /**
    * User email 
    */
    email: string
  }

  interface IVisitorInfo {
    /**
    * Visitor name 
    */
    name: string
    /**
    * Visitor email 
    */
    email: string
    /** 
    * Visitor phone (optional)
    */
    phone?: string
    /** 
    * Department to redirect chat (optional) 
    */
    department?: string
    /**
    * Tags to set for the chat to better redirect (optional) 
    */
    tags?: Array<string>
  }

  interface IArticlesFilter{
    /**
    * The type of the filter to be applied.
    */
    type: ArticlesFilterType
    /**
    * The filter values to be applied based on selected type.
    */
    values: Array<string>
  }

  /**
  * Choose what chat menu action items are enabled
  */
  interface IChatMenuActions {
    /**
    * Enable chat end menu item
    * @default true
    */
    enableEndChatMenuItem?: boolean
    /**
    * Enable chat transcript menu item
    * @default true
    */
    enableChatTranscriptMenuItem?: boolean
  }

  /**
  * In case you have enabled pre chat forms, you can configure what fields to be required, optional or hidden.
  * Read more about them here:
  * https://developer.zendesk.com/embeddables/docs/chat-sdk-v-2-for-ios/customize_the_look#configuring-a-pre-chat-form
  */
  interface IPreChatFormOptions {
    /**
    * Pre chat ask for name option
    * @default "OPTIONAL"
    */
    name?: PreChatFormFieldStatusType
    /**
    * Pre chat ask for email option
    * @default "OPTIONAL"
    */
    email?: PreChatFormFieldStatusType
    /**
    * Pre chat ask for phone option
    * @default "OPTIONAL"
    */
    phone?: PreChatFormFieldStatusType
    /**
    * Pre chat ask for department option
    * @default "OPTIONAL"
    */
    department? : PreChatFormFieldStatusType
  }

  /**
  * Default options to configure the chat.
  * Read more about them here: 
  * https://developer.zendesk.com/embeddables/docs/chat-sdk-v-2-for-ios/customize_the_look#customizing-the-chat-experience
  */
  interface IChatConfigurationOptions {
    /**
    * Agent availability is a feature that prevents the visitor from sending messages when there are no agents online. 
    * Enabling this feature will prevent your business from getting missed chats.
    * @default true
    */
    enableAgentAvailability?: boolean
    /**
    * Toggles the flag to either enable or disable the email transcript prompt at the end of a chat that allows end users to request a transcript.
    * @default true
    */
    enableChatTranscriptPrompt?: boolean
    /**
    * Offline form is an alternative feature to live chat that can be used to contact the business when all agents are unavailable while avoiding missed chats.
    * Similarly to pre-Chat, the offline form also collects information about the visitor in a conversational manner prior to sending the form.
    * @default true
    */
    enableOfflineForm?: boolean
    /**
    * Pre-Chat form is a feature that collects information about the visitor in a conversational manner prior to starting the chat.
    * If true, you can also set specific fields to be required, optional or hidden in pre chat.
    * @default true
    */
    enablePreChatForm?: boolean
    /**
    * In case you have enabled pre chat forms, you can configure what fields to be required, optional or hidden
    */
    preChatFormOptions?: IPreChatFormOptions
    /**
    * Allows overriding of the conversation menu. By default the menu contains all ChatMenuAction items.
    * @default true
    */
    enableChatMenuActions?: boolean
    /**
    * In case you have enabled chat menu actions, you can configure what menu items are enabled
    */
    chatMenuActions?: IChatMenuActions
  }

  /**
  * Define all chat and help center common used options
  */
  interface ICommonOptions {
    /** 
    * The user identity to start chat and help center (optional) 
    */
    userIdentity?: IUserIdentity
    /** 
    * The visitor info to start chat and help center (optional) 
    */
    visitorInfo?: IVisitorInfo
}
  
  /**
  * Define all chat options
  */
  interface IChatOptions extends ICommonOptions {
    /** 
    * Chat toolbar title (Android only) 
    */
    toolbarTitle?: string
    /** 
    * Chat close button text (iOS only)
    */
    closeButtonText?: string
    /** 
    * The alias name of the chat bot (optional)
    * @default 'Answer Bot'
    */
    botName?: string
    /**
    * True - use only chat SDK in chat
    * False - use all unified SDKs in chat (answer bot, support and chat)
    * @default false
    */
    chatOnly?: boolean
    /**
    * Default options to configure the chat (optional).
    */
    chatConfigurationOptions?: IChatConfigurationOptions
  }

  /**
  * Define all help center options
  */
  interface IHelpCenterOptions extends ICommonOptions {
    /**
    * Whether you also want the help center to give live chat option at bottom (optional)
    */
    withChat?: boolean
    /**
    * In case you want to not let users create tickets (optional)
    */
    disableTicketCreation?: boolean
    /**
    * Apply filters to articles list by category, section or label types.
    * Add an array with the respective values from the selected type.
    */
    articlesFilter?: IArticlesFilter
  }

  /**
  * To initialize the Zendesk with all SDK unified capabilities
  * @param initializationOptions
  */
  export function init(initializationOptions: ISDKInitOptions): void;

  /**
  * To initialize the chat SDK only, to be used only if you just want chat SDK
  * @param accountKey 
  */
  export function initChat(accountKey: string): void;

  /**
  * Function to reset user identify information
  */
  export function resetUserIdentity(): void;

  /**
  * Set the Identity for the SDK. If there is a previously stored Identity and this sets different Identity any user data will be wiped from the SDK.
  * userIdentity can be set multiple times with different valid identities.
  * @param userIdentity 
  */
  export function setUserIdentity(userIdentity: IUserIdentity): void;

  /**
  * Sets the information about the visitor.
  * Visitor information persists across chat sessions.
  * @param visitorInfo 
  */
  export function setVisitorInfo(visitorInfo: IVisitorInfo): void;

  /**
  * Set chat primary color (iOS only)
  * @param color 
  */
  export function setPrimaryColor(color: string): void;

  /**
  * Function to register your notification token with Zendesk, to receive notifications on device
  * @param token 
  */
  export function setNotificationToken(token: string): void;

  /**
  * To start a chat session
  * @param chatOptions
  */
  export function startChat(chatOptions: IChatOptions): void;

  /**
  * To show help center from Zendesk Support SDK, can also have chat feature
  * @param helpCenterOptions 
  */
  export function showHelpCenter(helpCenterOptions: IHelpCenterOptions): void;
}