package com.nabla.health.sdk.playground.injection

import com.nabla.health.sdk.messaging.core.injection.MessagingContainer
import com.nabla.health.sdk.messaging.ui.injection.MessagingUiContainer

class AppContainer {
    val messagingContainer = MessagingContainer()
    val messagingUiContainer = MessagingUiContainer(messagingContainer)
}

val appContainer by lazy { AppContainer() }
