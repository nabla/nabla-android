package com.nabla.sdk.messaging.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.flow.Flow

internal interface ConversationRepository {
    suspend fun createConversation(
        title: String?,
        providerIds: List<Uuid>?,
        initialMessage: MessageInput?
    ): Conversation
    fun createLocalConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local
    fun watchConversation(conversationId: ConversationId): Flow<Conversation>
    fun watchConversations(): Flow<PaginatedList<Conversation>>
    suspend fun loadMoreConversations()
    suspend fun markConversationAsRead(conversationId: ConversationId)
}
