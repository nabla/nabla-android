package com.nabla.sdk.messaging.core.data.conversation

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.data.message.GqlConversationContentDataSource
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

internal class ConversationRepositoryImpl(
    repoScope: CoroutineScope,
    private val gqlConversationDataSource: GqlConversationDataSource,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
) : ConversationRepository {

    private val loadMoreConversationSharedSingle: SharedSingle<Unit> = sharedSingleIn(repoScope) {
        gqlConversationDataSource.loadMoreConversationsInCache()
    }

    override suspend fun createConversation(
        title: String?,
        providerIds: List<Uuid>?,
    ): Conversation {
        return gqlConversationDataSource.createConversation(title, providerIds)
    }

    @OptIn(FlowPreview::class)
    override fun watchConversation(conversationId: ConversationId): Flow<Conversation> {
        return flowOf(
            gqlConversationContentDataSource.conversationEventsFlow(conversationId),
            gqlConversationDataSource.watchConversation(conversationId)
        )
            .flattenMerge()
            .filterIsInstance()
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
