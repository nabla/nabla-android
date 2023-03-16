#  Nabla Android SDK
[![Release](https://jitpack.io/v/nabla/nabla-android.svg)](https://jitpack.io/#Nabla/nabla-android)

The [Nabla](https://www.nabla.com/) Android SDK makes it quick and easy to build an excellent healthcare communication experience in your Android app. We provide powerful and customizable UI elements that can be used out-of-the-box to create a full healthcare communication experience. We also expose the low-level APIs that power those UIs so that you can build fully custom experiences.

Right now the library is in alpha, meaning all core features are here and ready to be used but API might change during the journey to a stable 1.0 version.

## Documentation

Check our [documentation portal](https://docs.nabla.com/docs/concepts-android) for in depth documentation about integrating the SDK.

## Getting started

The Nabla Messaging UI SDK is compatible with Android 6 (API Level 23) and higher.

First add Jitpack repository to your build file if it's not already there:
```
dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Then add Nabla's dependency in your app's build.gradle. You can choose between:

- The `messaging-ui` artifact that comes with our ready-to-use UI components

```
implementation 'com.nabla.nabla-android:messaging-ui:latestVersion'
```

- The `messaging-core` artifact that exposes the low-level APIs only

```
implementation 'com.nabla.nabla-android:messaging-core:latestVersion'
```


You can find the latest version available in the [release page](https://github.com/nabla/nabla-android/releases).

## Sample app

You can find an example of a basic integration of the Messaging UI SDK in our [messaging sample app](https://github.com/nabla/nabla-android/tree/main/messaging-sample-app).

## Need more help?

If you need any help with the set-up of the SDK or the Nabla platform, please contact us on [our website](https://nabla.com). We are available to answer any question.
