package com.nabla.sdk.messaging.core.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.CacheMissException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.mapper.Mapper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ConversationRepositoryImpl(
    private val logger: Logger,
    private val apolloClient: ApolloClient,
    private val mapper: Mapper,
) : ConversationRepository {

    private val loadMoreConversationsMutex = Mutex()

    override suspend fun createConversation() {
        // Stub
        logger.debug("createConversation")
    }

    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        val query = firstConversationsPageQuery()
        return apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val items = queryData.conversations.conversations.map {
                    mapper.mapToConversation(it.conversationListItemFragment)
                }
                return@map PaginatedList(items, queryData.hasNextPage())
            }
    }

    override suspend fun loadMoreConversations() {
        loadMoreConversationsMutex.withLock {
            // Avoid concurrent updates.
            // More complex policy could involve checking last update time, or re attach somehow
            // concurrent loadMoreConversations().
            loadMoreConversationOperation()
        }
    }

    private suspend fun loadMoreConversationOperation() {
        val firstPageQuery = firstConversationsPageQuery()
        val cachedQueryData = try {
            apolloClient.apolloStore.readOperation(firstPageQuery)
        } catch (cacheMissException: CacheMissException) {
            null
        } ?: return
        if (!cachedQueryData.hasNextPage()) return
        val updatedQuery = firstPageQuery.copy(
            cursor = OpaqueCursorPage(
                cursor = Optional.presentIfNotNull(cachedQueryData.conversations.nextCursor)
            )
        )
        val freshQueryData = apolloClient.query(updatedQuery)
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val mergedConversations =
            (cachedQueryData.conversations.conversations + freshQueryData.conversations.conversations)
                .distinctBy { it.conversationListItemFragment.id }
        val mergedQueryData = freshQueryData.copy(
            conversations = freshQueryData.conversations.copy(
                conversations = mergedConversations
            )
        )
        apolloClient.apolloStore.writeOperation(firstPageQuery, mergedQueryData)
    }

    private fun ConversationListQuery.Data.hasNextPage(): Boolean {
        // TODO : Update impl according opaque cursor schema
        return conversations.nextCursor != null
    }

    private fun firstConversationsPageQuery() =
        ConversationListQuery(OpaqueCursorPage(cursor = Optional.Absent))

}
