package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.fake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MessageRepositoryMock(private val logger: Logger) : MessageRepository {
    override fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
        return flowOf(ConversationWithMessages.fake())
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        logger.debug("load more messages")
    }

    override suspend fun sendMessage(message: Message) {
        TODO("Not yet implemented")
    }
}
