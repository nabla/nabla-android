package com.nabla.sdk.messaging.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.flow.Flow

@NablaInternal
public interface ConversationRepository {
    public suspend fun createConversation(
        title: String?,
        providerIds: List<Uuid>?,
        initialMessage: MessageInput?,
    ): Conversation

    public fun createLocalConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local
    public fun watchConversation(conversationId: ConversationId): Flow<Conversation>
    public fun watchConversations(): Flow<PaginatedList<Conversation>>
    public suspend fun loadMoreConversations()
    public suspend fun markConversationAsRead(conversationId: ConversationId)
}
