package com.nabla.sdk.messaging.core.domain.boundary

import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun createConversation()
    fun getConversations(): Flow<List<Conversation>>
}
