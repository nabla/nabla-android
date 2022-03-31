package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow

internal class ConversationRepositoryImpl(private val logger: Logger) : ConversationRepository {
    override suspend fun createConversation() {
        // Stub
        logger.debug("createConversation")
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        TODO("Not yet implemented")
    }

    override suspend fun loadMoreConversations() {
        logger.debug("load more conversations")
    }
}
