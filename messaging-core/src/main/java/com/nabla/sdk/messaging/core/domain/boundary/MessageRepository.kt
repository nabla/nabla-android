package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages>
    suspend fun loadMoreMessages(conversationId: ConversationId)
    suspend fun sendMessage(message: Message)
    suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local)
    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean)
    suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId)
}
