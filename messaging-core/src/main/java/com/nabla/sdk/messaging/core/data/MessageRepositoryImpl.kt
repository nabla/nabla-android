package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import kotlinx.coroutines.flow.Flow

internal class MessageRepositoryImpl(private val logger: Logger) : MessageRepository {
    override fun watchConversationMessages(conversationId: Id): Flow<ConversationWithMessages> {
        TODO("Not yet implemented")
    }

    override suspend fun loadMoreMessages(conversationId: Id) {
        TODO("Not yet implemented")
    }
}
