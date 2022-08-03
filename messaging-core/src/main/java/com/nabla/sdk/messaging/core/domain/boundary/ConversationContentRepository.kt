package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.messaging.core.data.message.PaginatedConversationItems
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.flow.Flow

@NablaInternal
public interface ConversationContentRepository {
    public fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems>
    public suspend fun loadMoreMessages(conversationId: ConversationId)
    public suspend fun sendMessage(input: MessageInput, conversationId: ConversationId, replyTo: MessageId.Remote?): MessageId.Local
    public suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local)
    public suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean)
    public suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId)
}
