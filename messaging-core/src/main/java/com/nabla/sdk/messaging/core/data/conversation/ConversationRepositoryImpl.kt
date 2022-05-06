package com.nabla.sdk.messaging.core.data.conversation

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal class ConversationRepositoryImpl(
    repoScope: CoroutineScope,
    private val gqlConversationDataSource: GqlConversationDataSource,
) : ConversationRepository {

    private val loadMoreConversationSharedSingle: SharedSingle<Unit> = sharedSingleIn(repoScope) {
        gqlConversationDataSource.loadMoreConversationsInCache()
    }

    override suspend fun createConversation(): Conversation {
        return gqlConversationDataSource.createConversation()
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Conversation> {
        return gqlConversationDataSource.watchConversation(conversationId)
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return gqlConversationDataSource.watchConversations()
    }

    override suspend fun loadMoreConversations() {
        loadMoreConversationSharedSingle.await()
    }

    override suspend fun markConversationAsRead(conversationId: ConversationId) {
        gqlConversationDataSource.markConversationAsRead(conversationId)
    }
}
