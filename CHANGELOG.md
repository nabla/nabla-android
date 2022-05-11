# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- This CHANGELOG file

### Changed
- `NablaCore` is now called `NablaClient`, `NablaMessaging` is now called `NablaMessagingClient`
- `NablaCore.initialize` now doesn't require a `SessionTokenProvider`, it must now be passed to `NablaCore.authenticate` along with the `userId`
- Some `messaging-ui` strings have been updated
- `NablaMessagingClient.watchConversationMessages` now returns a `ConversationMessages` object which only contains the messages themself. Details of the conversation and its participants should now be watched using `NablaMessagingClient.watchConversation`
- Some resources of `messaging-ui` and `messaging-core` have been prefixed correctly with `nabla_`

## [1.0-alpha02] - 2022-05-03

### Fixed
- `messaging-ui` artifact would not expose `messaging-core` apis, preventing an app relying only on `messaging-ui` from building
- Internal exception mapping sometimes returning `NablaException.Unknown` for known cases

## [1.0-alpha01] - 2022-05-02

### Added
- First public version of the SDK
