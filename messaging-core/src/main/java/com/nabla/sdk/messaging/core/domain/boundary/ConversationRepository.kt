package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun createConversation()
    fun watchConversations(): Flow<PaginatedList<Conversation>>
    suspend fun loadMoreConversations()
    fun watchConversation(id: Id): Flow<PaginatedList<Message>>
}
