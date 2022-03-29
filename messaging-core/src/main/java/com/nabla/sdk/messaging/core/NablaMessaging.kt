package com.nabla.sdk.messaging.core

import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

class NablaMessaging private constructor() {
    private val messagingContainer = MessagingContainer()

    val conversationRepository = messagingContainer.conversationRepository

    fun watchConversations(): Flow<List<Conversation>> {
        return messagingContainer.conversationRepository.watchConversations()
    }

    companion object {
        val instance = NablaMessaging()
    }
}
