package com.nabla.sdk.messaging.core

import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.entity.ConfigurationException

public interface NablaMessagingModule {
    public companion object Factory {
        public operator fun invoke(): MessagingModule.Factory =
            MessagingModule.Factory { MessagingModuleImpl(coreContainer = it) }
    }
}

public val NablaClient.messagingClient: NablaMessagingClient
    get() = coreContainer.messagingModule as? NablaMessagingClient
        ?: throw ConfigurationException.ModuleNotInitialized("NablaMessagingModule")
