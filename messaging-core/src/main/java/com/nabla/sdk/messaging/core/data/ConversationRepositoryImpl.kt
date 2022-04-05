package com.nabla.sdk.messaging.core.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.CacheMissException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.asSharedSingleIn
import com.nabla.sdk.graphql.ConversationListQuery
import com.nabla.sdk.graphql.ConversationQuery
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.mapper.Mapper
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

internal class ConversationRepositoryImpl(
    private val logger: Logger,
    private val apolloClient: ApolloClient,
    private val mapper: Mapper,
) : ConversationRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMoreConversationSharedSingle: SharedSingle<Unit> =
        ::loadMoreConversationOperation.asSharedSingleIn(repoScope)
    private val conversationsEventsFlow = apolloClient.subscription(ConversationsEventsSubscription())
        .toFlow()
        .onEach {
            onConversationsEvent(it.dataAssertNoErrors)
        }.shareIn(
            scope = repoScope,
            replay = 0,
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)
        )

    private suspend fun onConversationsEvent(data: ConversationsEventsSubscription.Data) {
        data.conversations.onConversationCreatedEvent?.conversation?.let {
            insertConversationToConversationsListCache(it)
        }
    }

    private suspend fun insertConversationToConversationsListCache(
        conversation: ConversationsEventsSubscription.Conversation
    ) {
        val firstPageQuery = firstConversationsPageQuery()
        val cachedQueryData = try {
            apolloClient.apolloStore.readOperation(firstPageQuery)
        } catch (cacheMissException: CacheMissException) {
            null
        } ?: return
        val newItem = ConversationListQuery.Conversation(
            conversation.__typename,
            conversation.conversationFragment
        )
        val mergedConversations = listOf(newItem) + cachedQueryData.conversations.conversations
        val mergedQueryData = cachedQueryData.copy(
            conversations = cachedQueryData.conversations.copy(
                conversations = mergedConversations
            )
        )
        apolloClient.apolloStore.writeOperation(firstPageQuery, mergedQueryData)
    }

    override suspend fun createConversation() {
        // Stub
        logger.debug("createConversation")
    }

    @OptIn(FlowPreview::class)
    override fun watchConversations(): Flow<PaginatedList<Conversation>> {
        val query = firstConversationsPageQuery()
        val dataFlow = apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val items = queryData.conversations.conversations.map {
                    mapper.mapToConversation(it.conversationFragment)
                }
                return@map PaginatedList(items, queryData.hasNextPage())
            }
        return flowOf(conversationsEventsFlow, dataFlow).flattenMerge().filterIsInstance()
    }

    override suspend fun loadMoreConversations() {
        loadMoreConversationSharedSingle.await()
    }

    override fun watchConversation(id: Id): Flow<PaginatedList<Message>> {
        val query = firstMessagePageQuery(id)
        val dataFlow = apolloClient.query(query)
            .watch()
            .map { response -> response.dataAssertNoErrors }
            .map { queryData ->
                val page = queryData.conversation.conversation.conversationMessagesPageFragment.items
                val items = page.data.mapNotNull { it?.messageFragment }.map {
                    mapper.mapToMessage(it)
                }
                return@map PaginatedList(items, page.hasMore)
            }
        return dataFlow
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
            pageInfo = OpaqueCursorPage(
                cursor = Optional.presentIfNotNull(cachedQueryData.conversations.nextCursor)
            )
        )
        val freshQueryData = apolloClient.query(updatedQuery)
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val mergedConversations =
            (cachedQueryData.conversations.conversations + freshQueryData.conversations.conversations)
                .distinctBy { it.conversationFragment.id }
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

    private fun firstMessagePageQuery(id: Id) =
        ConversationQuery(id.id, OpaqueCursorPage(cursor = Optional.Absent))
}
