package com.nabla.sdk.messaging.core.data.conversation

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.GQL_DOMAIN
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.graphql.type.SendMessageInput
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.messaging.core.data.apollo.notifyTypingUpdates
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.graphql.ConversationQuery
import com.nabla.sdk.messaging.graphql.ConversationsEventsSubscription
import com.nabla.sdk.messaging.graphql.ConversationsQuery
import com.nabla.sdk.messaging.graphql.CreateConversationMutation
import com.nabla.sdk.messaging.graphql.MaskAsSeenMutation
import com.nabla.sdk.messaging.graphql.fragment.ConversationFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock

internal class GqlConversationDataSource constructor(
    private val logger: Logger,
    coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
    private val clock: Clock,
) {
    private val conversationsEventsFlow by lazy {
        apolloClient.subscription(ConversationsEventsSubscription())
            .toFlow()
            .retryOnNetworkErrorAndShareIn(coroutineScope).onEach {
                logger.debug(domain = GQL_DOMAIN, message = "Event $it")
                it.dataOrThrowOnError.conversations?.event?.onConversationCreatedEvent?.conversation?.conversationFragment?.let {
                    insertConversationToConversationsListCache(it)
                }
            }
    }

    private suspend fun insertConversationToConversationsListCache(
        conversation: ConversationFragment,
    ) {
        apolloClient.updateCache(conversationsQuery()) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val isAlreadyInCache = cachedQueryData.conversations.conversations.any { it.conversationFragment.id == conversation.id }

            if (isAlreadyInCache) return@updateCache CacheUpdateOperation.Ignore()

            val newItem = ConversationsQuery.Conversation(
                com.nabla.sdk.graphql.type.Conversation.type.name,
                conversation
            )
            val mergedConversations = listOf(newItem) + cachedQueryData.conversations.conversations
            val mergedQueryData = cachedQueryData.modify(mergedConversations)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun loadMoreConversationsInCache() {
        apolloClient.updateCache(conversationsQuery()) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversations.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val updatedQuery = conversationsQuery(cachedQueryData.conversations.nextCursor)

            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataOrThrowOnError
            val mergedConversations =
                (cachedQueryData.conversations.conversations + freshQueryData.conversations.conversations)
                    .distinctBy { it.conversationFragment.id }
            val mergedQueryData = freshQueryData.modify(mergedConversations)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    @OptIn(FlowPreview::class)
    fun watchConversations(): Flow<PaginatedList<Conversation>> {
        val dataFlow = apolloClient.query(conversationsQuery())
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .watch(fetchThrows = true)
            .map { response -> response.dataOrThrowOnError }
            .map { queryData ->
                val items = queryData.conversations.conversations.map {
                    mapper.mapToConversation(it.conversationFragment)
                }.sortedByDescending { conversation -> conversation.lastModified }

                return@map PaginatedList(items, queryData.conversations.hasMore)
            }
        return flowOf(conversationsEventsFlow, dataFlow)
            .flattenMerge()
            .filterIsInstance()
    }

    suspend fun markConversationAsRead(conversationId: ConversationId.Remote) {
        apolloClient.mutation(MaskAsSeenMutation(conversationId.remoteId)).execute()
    }

    suspend fun createConversation(
        title: String?,
        providerIds: List<Uuid>?,
        initialMessage: SendMessageInput?,
        onSuccessSideEffect: ((remoteConversationUuid: Uuid) -> Unit)? = null,
    ): Conversation {
        val data = apolloClient.mutation(
            CreateConversationMutation(
                title = Optional.presentIfNotNull(title),
                providerIds = Optional.presentIfNotNull(providerIds),
                initialMessage = Optional.presentIfNotNull(initialMessage),
            )
        ).execute().dataOrThrowOnError

        onSuccessSideEffect?.invoke(
            data.createConversation.conversation.conversationFragment.id
        )

        return mapper.mapToConversation(data.createConversation.conversation.conversationFragment)
    }

    @OptIn(FlowPreview::class)
    fun watchConversation(conversationId: ConversationId.Remote): Flow<Conversation> {
        val watcher = apolloClient.query(ConversationQuery(conversationId.remoteId))
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true)
            .map { response -> response.dataOrThrowOnError }
            .notifyTypingUpdates(clock = clock) { data ->
                data.conversation.conversation.conversationFragment.providers
                    .map { it.providerInConversationFragment }
                    .map { mapper.mapToProviderInConversation(it) }
            }
            .map { queryData -> mapper.mapToConversation(queryData.conversation.conversation.conversationFragment) }

        return flowOf(conversationsEventsFlow, watcher)
            .flattenMerge()
            .filterIsInstance()
    }

    companion object {
        @VisibleForTesting
        internal fun conversationsQuery(pageCursor: String? = null) = ConversationsQuery(
            OpaqueCursorPage(
                cursor = Optional.presentIfNotNull(pageCursor),
                numberOfItems = Optional.Present(50),
            )
        )
    }
}
