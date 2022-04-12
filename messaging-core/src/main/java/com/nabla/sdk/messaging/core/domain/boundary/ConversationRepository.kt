package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.flow.Flow

internal interface ConversationRepository {
    suspend fun createConversation()
    fun watchConversations(): Flow<PaginatedList<Conversation>>
    suspend fun loadMoreConversations()
    suspend fun markConversationAsRead(conversationId: ConversationId)
}
