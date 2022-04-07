package com.nabla.sdk.messaging.core.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.watch
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.graphql.CreateConversationMutation
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlEventHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlHelper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlMapper
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class ConversationRepositoryImpl(
    private val logger: Logger,
    repoScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: MessagingGqlMapper,
    private val gqlOperationHelper: MessagingGqlOperationHelper,
    private val gqlEventHelper: MessagingGqlEventHelper,
) : ConversationRepository {

    private val loadMoreConversationSharedSingle: SharedSingle<Unit> = sharedSingleIn(repoScope) {
        gqlOperationHelper.loadMoreConversationsInCache()
    }

    override suspend fun createConversation() {
        apolloClient.mutation(CreateConversationMutation()).execute().dataAssertNoErrors
    }

    @OptIn(FlowPreview::class)
    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        val query = MessagingGqlHelper.firstConversationsPageQuery()
        val dataFlow = apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val items = queryData.conversations.conversations.map {
                    mapper.mapToConversation(it.conversationFragment)
                }
                return@map PaginatedList(items, queryData.conversations.hasMore)
            }
        return flowOf(gqlEventHelper.conversationsEventsFlow, dataFlow)
            .flattenMerge()
            .filterIsInstance()
    }

    override suspend fun loadMoreConversations() {
        loadMoreConversationSharedSingle.await()
    }
}
