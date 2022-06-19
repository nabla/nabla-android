# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Now `messaging-core` supports messages replying to other messages.
- You can now pass your own instance of `Logger` via the `Configuration` when initializing the SDK.
- Now on `ConversationFragment` you can reply to messages.

### Changed

- Media in conversations: Extend support to `image/*` and `audio/*` mime types.
- `Configuration` has been split into `NetworkConfiguration` and `Configuration`. This should not have an impact on an existing app as all the
  properties that have been moved are supposed to be for tests only.
- Increase timeout duration to 2 minutes for network operations (especially for uploads).

### Fixed
- Fix an issue where `NablaMessagingClient.watchConversation` wouldn't be called correctly in some cases when the conversation is updated.
- Fix duplicate activity item when a provider joins a conversation.
- Fix voice message recording on Android API < 26.
- Fix IndexOutOfBound when adding multiple media from gallery (as message attachment) with different mime types.

## [1.0-alpha04] - 2022-05-31

### Added

- `ConversationActivity` in conversations. A message is displayed when a provider joins a conversation.
- `ConversationFragment` now displays system messages, with the right name and avatar.
- Added support for sending, receiving and playing voice messages.

### Changed
- `MessageSender` is renamed to `MessageAuthor`.
- Documents name are now displayed only on 1 line when using `ConversationFragment`.
- `NablaMessagingClient.watchConversationMessages` replaced by `NablaMessagingClient.watchConversationItems`.
- `NablaMessagingClient.sendMessage` now takes a `MessageInput` and the `ConversationId` rather than the message directly.
- `FileLocal.Image` now takes an optional file name and the `MimeType` of the image as parameter, along with the `Uri`.
- `MessageSender.System` now exposes a `User.System` parameter which contains the name of the organization and the avatar url.
- Removed `SendStatus.ToBeSent` as it was never provided as a value by the SDK.

### Fixed
- Uploading an image captured from camera using `ConversationFragment` would fail, this is now fixed.
- In `ConversationListView`: Keep scroll at the top of conversations list when a new item is added.
- In `ConversationFragment`: Now we check the mime type in media picker intent if any before
  inferring one from the file itself.
- `NablaException.Authentication.NotAuthenticated` will now be correctly returned as a `Result.failure`
  for methods that require authentication and return a `Result`, rather than thrown.
- `NablaException.Authentication.NotAuthenticated` will now be correctly sent as an error in the `Flow`
  for methods that require authentication and return a `Flow`, rather than thrown.
- Fix usage of color selector as background not working on older android api levels.
- Fix media picker bottom sheet not fully shown when on landscape.
- Fix file message thumbnail placeholder having a 1px unwanted border.

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
