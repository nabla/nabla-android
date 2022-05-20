package com.nabla.sdk.uitests.scene

import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment

class StubbedConversionFragment : ConversationFragment() {
    override val messagingClient = nablaMessagingClient
}
