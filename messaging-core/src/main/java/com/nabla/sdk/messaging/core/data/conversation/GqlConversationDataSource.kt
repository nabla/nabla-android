package com.nabla.sdk.messaging.core.data.conversation

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.ApolloExt.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.ApolloExt.updateCache
import com.nabla.sdk.core.data.apollo.ApolloResponseExt.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.SubscriptionExt.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.SubscriptionExt.toFlowAsRetryable
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.GQL_DOMAIN
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.helper.ApolloResponseHelper.watchAsCachedResponse
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
    private val exceptionMapper: NablaExceptionMapper,
    private val clock: Clock,
) {
    private val conversationsEventsFlow by lazy {
        apolloClient.subscription(ConversationsEventsSubscription())
            .toFlowAsRetryable()
            .retryOnNetworkErrorAndShareIn(coroutineScope).onEach { response ->
                logger.debug(domain = GQL_DOMAIN, message = "Event ${response.data?.conversations?.event?.__typename}")
                response.errors?.forEach {
                    logger.error(domain = GQL_DOMAIN, message = "error received in ConversationsEventsSubscription: ${it.message}")
                }
                response.data?.conversations?.event?.onSubscriptionReadinessEvent?.let {
                    /* no-op */
                    return@onEach
                }
                response.data?.conversations?.event?.onConversationCreatedEvent?.conversation?.conversationFragment?.let { conversationFragment ->
                    insertConversationToConversationsListCache(conversationFragment)
                    return@onEach
                }
                response.data?.conversations?.event?.onConversationUpdatedEvent?.conversation?.conversationFragment?.let {
                    /* no-op — Handled by Apollo's Normalized Cache */
                    return@onEach
                }
                response.data?.conversations?.event?.onConversationDeletedEvent?.conversationId?.let { id ->
                    deleteFromConversationsCache(id)
                    return@onEach
                }
                logger.warn("Unknown ConversationsEventsSubscription event not handled: ${response.data?.conversations?.event?.__typename}")
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

    private suspend fun deleteFromConversationsCache(conversationId: Uuid) {
        apolloClient.updateCache(conversationsQuery()) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val isNotInCache = cachedQueryData.conversations.conversations.none { it.conversationFragment.id == conversationId }
            if (isNotInCache) return@updateCache CacheUpdateOperation.Ignore()

            val filteredConversations = cachedQueryData.conversations.conversations.filter { it.conversationFragment.id != conversationId }
            CacheUpdateOperation.Write(cachedQueryData.modify(filteredConversations))
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
    fun watchConversations(): Flow<Response<PaginatedList<Conversation>>> {
        val dataFlow = apolloClient.query(conversationsQuery())
            .watchAsCachedResponse(exceptionMapper)
            .map { response ->
                val items = response.data.conversations.conversations.map {
                    mapper.mapToConversation(it.conversationFragment)
                }.sortedByDescending { conversation -> conversation.lastModified }

                return@map Response(
                    isDataFresh = response.isDataFresh,
                    refreshingState = response.refreshingState,
                    data = PaginatedList(items, response.data.conversations.hasMore)
                )
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
    fun watchConversation(conversationId: ConversationId.Remote): Flow<Response<Conversation>> {
        val watcher = apolloClient.query(ConversationQuery(conversationId.remoteId))
            .watchAsCachedResponse(exceptionMapper)
            .notifyTypingUpdates(clock = clock) { response ->
                response.data.conversation.conversation.conversationFragment.providers
                    .map { it.providerInConversationFragment }
                    .map { mapper.mapToProviderInConversation(it) }
            }
            .map { response ->
                Response(
                    isDataFresh = response.isDataFresh,
                    refreshingState = response.refreshingState,
                    data = mapper.mapToConversation(response.data.conversation.conversation.conversationFragment)
                )
            }

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
