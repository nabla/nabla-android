# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.3] - 2023-05-17

Minor improvements to the caching implementation — no behaviour or API changes

## [1.1.2] - 2023-04-27

### Fixed
- Messaging UI: Fix a bug preventing `ConversationListView` from correctly binding itself to its ViewModel events in case of errors while fetching the data.
- Core: Fix a bug leading to new messages and items not being fetched in real time when using `clearCurrentUser`.

## [1.1.1] - 2023-04-25

### Fixed
- Core: Remove wrong caching of the value returned by `NablaClient.currentUserId`. It will now return the correct up-to-date value.

## [1.1.0] - 2023-04-18

### Added
- Messaging core: Added new `lastMessage` property on `Conversation` that gives access to the whole message and not just its preview.
- Add Portuguese translations

### Fixed
- Messaging: Make the button and the icon in video consultation item respect the container/content color pair.

## [1.0.1] - 2023-03-21

### Fixed
- Messaging: Fix joining/leaving multi-patients conversations not shown in real-time (but only on refresh).
- Messaging: Fix a rare UI glitch where the spacing between conversations on the `ConversationListView` would be too high.

## [1.0.0] - 2023-03-16

## [1.0.0-alpha26] - 2023-03-15

### Changed
- `AuthTokens` now takes `AccessToken` and `RefreshToken` as parameters to avoid any confusion. The order has been changed too, so you should now call `val authTokens = AuthTokens(AccessToken("your access token"), RefreshToken("your refreshToken"))`
- Messaging and Scheduling: Messaging core: Watchers will now correctly emit a UserIdNotSet exception if you call clearCurrentUser while still watching them.

### Fixed
- Fix a runtime issue with `ConversationListView` not binding properly to its view model if you called `bindViewModel` before attaching the view.

## [1.0.0-alpha25] - 2023-03-06

### Changed
- Scheduling: Replace your current usage of `nablaClient.schedulingModule` to `nablaClient.schedulingClient`.
- VideoCall: Replace your current usage of `nablaClient.videoCallModule` to `nablaClient.videoCallClient`.
- Core: `NablaClient.authenticate` is replaced by `NablaClient.setCurrentUserOrThrow` and `NablaClient.clearCurrentUser`. `SessionTokenProvider` is set during `NablaClient.initialize`.

### Added
- Scheduling: support for registering a payment step. See doc for details/instructions.

### Fixed
- Fix appointments list not updated in real-time for some edge-case status changes.

## [1.0.0-alpha24] - 2023-02-14

### Added
- Scheduling: Added a new `nablaScheduling_appointmentListHeaderStyle` for the `AppointmentsFragment` toolbar's style.

### Changed
- Scheduling: The `AppointmentsFragment` now comes with a `Toolbar` by default, you can choose to hide it by calling `setShowNavigation(false)` on the Fragment's builder.

## [1.0.0-alpha23] - 2023-02-03

### Added
- Core: Added a new `watchEventsConnectionState` method on `NablaClient` which allows you to monitor the current state of the network connection used to receive live events.
- Messaging Core: Added a new `Response` object returned by watchers. It contains metadata about the freshness of the data returned, allowing the caller to know if the data comes from cache or is fresh and if a background refresh is in progress.
- Messaging Core: Watchers should now automatically try to re-fetch fresh data when the device gets back online after being offline.

### Changed
- Messaging Core: `WatchPaginatedResponse` as been renamed `PaginatedContent`.
- Messaging Core: `watchConversation` now returns a `Flow<Response<Conversation>>`.
- Messaging Core: `watchConversationItems` now returns a `Flow<Response<WatchPaginatedResponse<List<ConversationItem>>>>`.
- Messaging Core: `watchConversations` now returns a `Flow<Response<WatchPaginatedResponse<List<Conversation>>>>`.

### Fixed
- Messaging UI: The `ConversationListView` will now correctly display the error state when it cannot fetch the data. It was showing a blank screen before.

### Fixed
- Scheduling: Fix a bug where a user could try to cancel a past appointment.

## [1.0.0-alpha22] - 2023-01-19

### Added
- Video Call: Better support for screen sharing.
- Added an extra step in the "schedule appointment" flow to choose between remote and physical appointments.
- Added an appointment detail view accessible from the list of appointments. For physical appointments, this view displays the address of the appointment.

### Changed
- Avatars are now using `colorSurfaceVariant` as default for background color. You can change that by customizing the `defaultBackgroundColor` of `Nabla.Widget.AvatarView`.
- Avatars will now display a default icon when no picture or abbreviated name are available. You can change that drawable by customizing the `defaultAvatarDrawable` of `Nabla.Widget.AvatarView`.
- Messaging UI: Messages from Providers and other users are now using `colorSurfaceVariant` as default background color. You can change that by customizing `nablaMessaging_providerMessageBackgroundColor`, `nablaMessaging_otherMessageBackgroundColor` and `nablaMessaging_deletedMessageBackgroundColor`
- Messaging UI: Messages from Providers and other users are now using `textColorPrimary` as default text color. You can change that by customizing `nablaMessaging_conversationProviderMessageAppearance` and  `nablaMessaging_conversationOtherMessageAppearance`
- Messaging UI: Removed the elevation for Providers and other users messages cell.

## [1.0-alpha21] - 2022-12-13

### Changed
- Messaging Core: Renamed `createDraftConversation` to `startConversation`. It keeps the behavior of creating the conversation lazily when the patient sends the first message.
- Messaging Core: `createConversation` has been renamed `createConversationWithMessage` and now has a required `message` argument. It should be used to start a conversation on behalf of the patient with a first message from them.

### Fixed
- Messaging UI: Fixed the play/pause button default color in dark mode for audio messages sent by a Provider.
- Messaging UI: Fixed the appearance used for other's (System or other Patient) image messages.

## [1.0-alpha20] - 2022-11-23

### Added
- Messaging UI: Add a new `setShowNavigation` method to `ConversationFragment`'s builder to hide the top navigation Toolbar in case you want to include the Fragment in your own navigation stack.
- Messaging UI: Add a new empty state when no conversations have been created yet for the patient. You can customize this text appearance using `nablaMessaging_conversationListEmptyStateTextAppearance`.

### Changed
- The SDK targets API 33, meaning you should bump your `compileSdkVersion` to be 33 or higher (this doesn't impact your app's minimum Android supported version).
- Android's map addresses detection and highlighting is now deprecated and its usage for text messages in Nabla Messaging UI has been removed.

## [1.0-alpha19] - 2022-11-16

### Added

- Support for dynamic consents in Scheduling module that you can customize from the console.

## Changed

- Dropped `nablaMessaging_conversationHeaderColor` in favor of the more generic `nablaMessaging_conversationHeaderStyle`.
- `com.google.android.material:material` updated from 1.5.0 to 1.6.0
- Removed the `showComposer` parameter from `ConversationFragment` & `ConversationActivity` and relies on `Conversation` `isLocked` property to hide the composer.  
⚠️ If you were using the `showComposer` parameter, it is not available anymore and you should migrate to using lock conversation from the Console.

### Fixed

- Fix SDK version sync with server failing in some rare cases.

## [1.0-alpha18] - 2022-11-03

> ⚠️ You need to add `vectorDrawables.useSupportLibrary true` in your `build.gradle` file under `defaultConfig` for your app if you target API 23 or lower starting with this release.

### Added

- New Messaging UI feature: You can now scan (with the camera) and send multi-page documents in conversations.
- Added support for group chats with multiple patients and providers.
- Added `onConversationHeaderClicked` callback on `ConversationFragment`.

### Fixed

- Messaging: Fix error when deleting a message on a freshly created draft conversation (i.e. with Local id).
- Messaging UI: Fix video thumbnail not showing in replied-to quoted message.
- Messaging UI: Fix file message title text appearance not fully applied from customization attribute.
- Messaging UI: Fix provider deleted message placeholder wrongly wrapped in a card-like container.
- Messaging UI: Fix conversation scroll state not restored after configuration change.
- Fix a crash on Android 6 and 7 when taking a picture with the camera in messaging UI module.
- Fix `NablaClient.authenticate` not re-throwing potential errors.

### Changed

- Messaging UI: Changed the default value of `nablaMessaging_conversationPreviewBackgroundDrawable` (background of a conversation item in the inbox) from a card to a transparent-with-ripple drawable.
- Messaging UI: Changed default spacing behavior for conversations-list recycler item decoration (`DefaultOffsetsItemDecoration`).
- Messaging UI: Replaced customization attribute `nablaMessaging_conversationListHeaderColor` by `nablaMessaging_conversationListHeaderStyle` which defaults to `?toolbarSurfaceStyle`.
- Messaging UI: Replaced —default values for— icons with Material equivalents (mic, send, trash bin, camera, video...).
- Messaging UI and Scheduling UI: minor theme styles and colors usage adjustments to fully support dark mode contracts.
- Messaging Core: breaking changes for the the structure of the sealed interface `MessageAuthor` and `Patient` entity to accommodate for current vs other patients. 

## [1.0-alpha17] - 2022-10-19

### Fixed

- Fix a crash introduced in alpha16 where sending or receiving a super large image could make the Conversation screen crash in messaging UI module.

## [1.0-alpha16] - 2022-10-18

### Changed

- Reporting: Add a module to report anonymous events to nabla servers to help debug some features like video calls.
- Upgraded dependency on Coil from 1.4.0 to 2.2.2.

### Fixed

- Fix kotlinx.serialization when obfuscated by excluding serializers from proguard.

## [1.0-alpha15] - 2022-10-13

### Fixed

- Authentication: Fix refresh token request broken since `1.0-alpha12` due to missing kotlinx.serialization.

## [1.0-alpha14] - 2022-10-11

### Changed

- Demo app has been renamed to `Messaging Sample App` to better reflect what it showcases.

### Added

- You can now define `nabla_defaultBackgroundColor` into `nablaAvatarViewStyle` in your theme to customize the default background color of avatars.

### Fixed

- Scheduling: last day's availabilities count (shown instead of slots grid when day is not expanded) was not updated after a new page is loaded.

## [1.0-alpha13] - 2022-09-22

### Changed

- `BookAppointmentActivity` has been renamed to `ScheduleAppointmentActivity`.
- `ScheduleAppointmentActivity.newIntent` is now internal. You can launch it using `nablaClient.schedulingModule.openScheduleAppointmentActivity(Context)`.
- Messaging Core `LivekitRoom` message type has been renamed to `VideoCallRoom`.

## [1.0-alpha12] - 2022-09-14

### Added

- New scheduling feature module.
- New `title` field on Provider.

### Changed

- Data class `WatchPaginatedResponse` moved from messaging to core package.
- Deleting `ConversationItems` and replacing usage with a `List<ConversationItem>`.

## [1.0-alpha11] - 2022-09-12

### Fixed

- Pagination for conversations and messages now has a more reasonable value (i.e. 50) and errors in page loading are properly handled.

## [1.0-alpha10] - 2022-08-19

### Added

- `video-call` module is now available to add Video calls to your messaging experience

### Changed
- A new `modules` parameter is now required when calling `NablaClient.initialize()`:
```kotlin
NablaClient.initialize(
    modules = listOf(
        NablaMessagingModule(),
    ),
)
```

## [1.0-alpha09] - 2022-08-04

### Changed

- Clicking new conversation in `InboxFragment` does not immediately create a conversation anymore. It now opens a draft conversation that won't be created until a first message is sent.
- `ConversationId` is now a sealed class that can be either `Local` or `Remote`

### Added

- Added draft conversations: You can now create a draft conversation and reference it by the returned id as you used to reference a normal conversation. The draft conversation will exist only locally until a first message is sent — it will then be created for real.

- Server-made i18n will now follow user's device language.

### Fixed

- Default color for hyperlinks had a bad contrast with the default background color of patient messages.

## [1.0-alpha08] - 2022-07-19

### Added

- Added `AuthenticationException.AuthorizationDenied` exception that can be return if the patient is not
authorized to access the data. This should not happen in the current implementation.

### Changed

- Conversations in `watchConversations()` are now correctly sorted by their `lastModified` date.
- `NablaClient` is exposed through static call `NablaClient.getInstance(name)`.

## [1.0-alpha07] - 2022-07-04

### Added

- Added a new `setShowComposer` method to `ConversationFragment.Builder` that you can call to hide the message composer for the patient.
- `InboxFragment` a standalone fragment for displaying the list of conversations with a button to create a new conversation.
- `ConversationActivity` a lightweight wrapper around `ConversationFragment`, convenient for default navigation behavior.

### Changed

- Remove the `description` field from `Conversation` and replace it by a `subtitle` that is displayed by the `ConversationFragment`
- Replaced `providerIdToAssign` by a list a `providerIds` in `NablaMessagingClient.createConversation`.

## [1.0-alpha06] - 2022-06-21

### Fixed

- Fix test fixtures being imported outside of tests, causing Gradle sync to fail on 1.0.0-alpha05 release.

### Changed

- Use `colorPrimary` in `NablaAvatarView` when no user instead of hardcoded color.

## [1.0-alpha05] - 2022-06-21

### Added

- Now `messaging-core` supports messages replying to other messages.
- Now `messaging-core` supports video messages.
- Now on `ConversationFragment` you can reply to messages.
- Now on `ConversationFragment` you can receive, play and send video messages, either from library or by recording a new one.
- You can now pass your own instance of `Logger` via the `Configuration` when initializing the SDK.
- Added optional `title` and `providerIdToAssign` arguments to `NablaMessagingClient.createConversation`.
- `NablaClient.initialize` now logs a `warning` in case of multiple calls.

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
- Fix error handling where the SDK would return a `NablaError.Unknown` instead of `NablaError.Server` in some cases.

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
