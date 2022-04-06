package com.nabla.sdk.messaging.core

import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

class NablaMessaging private constructor(coreContainer: CoreContainer) {

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.apolloClient
    )

    val conversationRepository: ConversationRepository = messagingContainer.conversationRepository

    fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return messagingContainer.conversationRepository.watchConversations()
    }

    companion object {
        val instance = NablaMessaging(NablaCore.instance.coreContainer)
    }
}
