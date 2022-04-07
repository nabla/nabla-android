package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun watchConversationMessages(conversationId: Id): Flow<ConversationWithMessages>
    suspend fun loadMoreMessages(conversationId: Id)
    suspend fun sendMessage(conversationId: Id, message: Message)
}
