package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.core.domain.entity.PaginatedResult
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun createConversation()
    suspend fun getConversationsPage(cursor: String?): PaginatedResult<Conversation>
    fun watchConversations(): Flow<List<Conversation>>
}
