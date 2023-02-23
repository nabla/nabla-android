package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.ui.helpers.FlowCollectorExtension.emitIn
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId

internal class InboxViewModel(
    private val messagingClient: NablaMessagingClient,
) : ViewModel() {
    private val openConversationMutableFlow = MutableLiveFlow<ConversationId>()
    internal val openConversationFlow: LiveFlow<ConversationId> = openConversationMutableFlow

    internal fun createConversation() {
        val draftConversationId = messagingClient.startConversation()
        openConversationMutableFlow.emitIn(viewModelScope, draftConversationId)
    }
}
