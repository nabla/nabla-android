package com.nabla.sdk.messaging.ui.injection

import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel

class MessagingUiContainer(
    private val conversationRepository: ConversationRepository
) {
    fun createConversationListViewModel(): ConversationListViewModel {
        return ConversationListViewModel(conversationRepository)
    }
}
