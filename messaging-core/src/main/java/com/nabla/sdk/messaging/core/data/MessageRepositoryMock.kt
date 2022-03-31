package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.fake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class MessageRepositoryMock(private val logger: Logger) : MessageRepository {
    override fun watchConversationMessages(conversationId: Id): Flow<ConversationWithMessages> {
        return flowOf(ConversationWithMessages.fake())
    }

    override suspend fun loadMoreMessages(conversationId: Id) {
        logger.debug("load more messages")
    }
}
