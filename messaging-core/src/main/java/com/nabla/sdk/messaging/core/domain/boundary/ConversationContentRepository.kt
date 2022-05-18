package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.messaging.core.data.message.PaginatedConversationItems
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.flow.Flow

internal interface ConversationContentRepository {
    fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems>
    suspend fun loadMoreMessages(conversationId: ConversationId)
    suspend fun sendMessage(input: MessageInput, conversationId: ConversationId): MessageId.Local
    suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local)
    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean)
    suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId)
}
