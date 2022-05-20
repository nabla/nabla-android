# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- `ConversationActivity` in conversations. A message is displayed when a provider joins a conversation.
- `ConversationFragment` now displays system messages, with the right name and avatar.

### Changed
- Documents name are now displayed only on 1 line when using `ConversationFragment`
- `NablaMessagingClient.watchConversationMessages` replaced by `NablaMessagingClient.watchConversationItems`.
- `NablaMessagingClient.sendMessage` now takes a `MessageInput` and the `ConversationId` rather than the message directly
- `FileLocal.Image` now takes an optional file name and the `MimeType` of the image as parameter, along with the `Uri`
- `MessageSender.System` now exposes a `User.System` parameter which contains the name of the organization and the avatar url.

### Fixed
- Uploading an image captured from camera using `ConversationFragment` would fail, this is now fixed

### Fixed
- In `ConversationListView`: Keep scroll at the top of conversations list when a new item is added.
- In `ConversationFragment`: Now we check the mime type in media picker intent if any before
  inferring one from the file itself.

## [1.0-alpha03] - 2022-05-16

### Added
- This CHANGELOG file
- Proguard configuration through `consumerProguardFiles` config.

### Changed
- `NablaCore` is now called `NablaClient`, `NablaMessaging` is now called `NablaMessagingClient`
- `NablaCore.initialize` now doesn't require a `SessionTokenProvider`, it must now be passed
  to `NablaCore.authenticate` along with the `userId`
- Some `messaging-ui` strings have been updated
- `NablaMessagingClient.watchConversationMessages` now returns a `ConversationMessages` object which
  only contains the messages themself. Details of the conversation and its participants should now
  be watched using `NablaMessagingClient.watchConversation`
- Some resources of `messaging-ui` and `messaging-core` have been prefixed correctly with `nabla_`
- `NablaException.Authentication` is now clearer with new `NotAuthenticated` and `UnableToGetFreshSessionToken` exceptions

### Fixed
- `ConversationFragment` was initializing default nabla client even if overridden with custom
  instance.
- Remove the ability to copy or delete a deleted message when using `ConversationFragment`

## [1.0-alpha02] - 2022-05-03

### Fixed
- `messaging-ui` artifact would not expose `messaging-core` apis, preventing an app relying only on `messaging-ui` from building
- Internal exception mapping sometimes returning `NablaException.Unknown` for known cases

## [1.0-alpha01] - 2022-05-02

### Added
- First public version of the SDK
