package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages>
    suspend fun loadMoreMessages(conversationId: ConversationId)
    suspend fun sendMessage(message: Message)
}
