package com.nabla.sdk.messaging.core

import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

class NablaMessaging {

    private val messagingContainer = MessagingContainer()

    val conversationRepository = messagingContainer.conversationRepository

    fun getConversations(): Flow<List<Conversation>> {
        return messagingContainer.conversationRepository.getConversations()
    }

    companion object {
        val instance = NablaMessaging()
    }
}
