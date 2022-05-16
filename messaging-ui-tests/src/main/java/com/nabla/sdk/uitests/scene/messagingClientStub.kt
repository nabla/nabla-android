package com.nabla.sdk.uitests.scene

import androidx.annotation.VisibleForTesting
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.data.stubs.NablaMessagingClientStub

@VisibleForTesting
val nablaMessagingClientStub = NablaMessagingClientStub()

val nablaMessagingClient: NablaMessagingClient
    get() = nablaMessagingClientStub
