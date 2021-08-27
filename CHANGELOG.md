# Changelog

All notable changes to this project will be documented in this file.
## [Version 0.3.0](https://github.com/livup/react-native-zendesk/compare/0.2.2...0.3.0) - 2021-08-27

### **Typescript**
- Add and improve typescript definitions and documentation
- Add new interfaces and enums to reflect the Zendesk API structure

### **Android and iOS**
- Add more configuration options in chat
- Add resetUserIdentity function
- Add support for filtering help center articles (by sections, categories and labels)
- Add support for configuring preChatOptions and other chat defaults
- Set userIdentity and visitorInfo when open chat or help center
- Better and more optimized code
- Add logs

### **Only iOS**
- Add support for close button text configuration to chat window
- Remove ZendeskSupportSDK fixed version from podspec

### **Only Android**
- Add support for toolbarTitle chat window configuration
- Enable jetifier and androidX
- Bump Zendesk SDK versions
- Bump gradle version to 7.0.0
- Bump minSdkVersion to 21
- Bump targetSdkVersion and compileSdkVersion to 31

## [Version 0.1.8](https://github.com/livup/react-native-zendesk/releases/tag/0.1.8)
- Fixed some issues in help center

## Version 0.1.7
- Fixed some issues
- Added support for global method to change primary color in iOS
- Added feature to disable ticket creation in help center.
