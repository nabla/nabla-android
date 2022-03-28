package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal class ConversationRepositoryImpl(): ConversationRepository {
    override suspend fun createConversation() {
        // Stub
    }

    override fun getConversations(): Flow<List<Conversation>> {
        // Stub
        return emptyFlow()
    }
}
