package com.nabla.sdk.messaging.ui

import com.nabla.sdk.messaging.core.Nabla
import com.nabla.sdk.messaging.ui.injection.MessagingUiContainer

// For the sake of simplicity we are providing whole container. We might restrict this later by only
// providing specific classes from within the container to avoid any usage of details of impl
// outside from SDK.
val Nabla.messagingUiContainer
    get() = MessagingUiContainer(this.messagingContainer)
