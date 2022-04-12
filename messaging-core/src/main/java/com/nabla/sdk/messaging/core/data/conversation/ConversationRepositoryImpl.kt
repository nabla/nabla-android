package com.nabla.sdk.messaging.core.data.conversation

import com.apollographql.apollo3.ApolloClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.graphql.CreateConversationMutation
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

internal class ConversationRepositoryImpl(
    private val logger: Logger,
    repoScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val gqlConversationDataSource: GqlConversationDataSource,
) : ConversationRepository {

    private val loadMoreConversationSharedSingle: SharedSingle<Unit> = sharedSingleIn(repoScope) {
        gqlConversationDataSource.loadMoreConversationsInCache()
    }

    override suspend fun createConversation() {
        apolloClient.mutation(CreateConversationMutation()).execute().dataAssertNoErrors
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
