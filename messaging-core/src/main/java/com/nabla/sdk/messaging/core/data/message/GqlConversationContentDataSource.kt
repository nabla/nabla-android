package com.nabla.sdk.messaging.core.data.message

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.readFromCache
import com.nabla.sdk.core.data.apollo.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.GQL_DOMAIN
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.helper.watchAsCachedResponse
import com.nabla.sdk.graphql.type.Conversation
import com.nabla.sdk.graphql.type.ConversationActivity
import com.nabla.sdk.graphql.type.DeletedMessageContent
import com.nabla.sdk.graphql.type.EmptyObject
import com.nabla.sdk.graphql.type.Message
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.graphql.type.SendMessageInput
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.graphql.ConversationEventsSubscription
import com.nabla.sdk.messaging.graphql.ConversationItemsQuery
import com.nabla.sdk.messaging.graphql.ConversationQuery
import com.nabla.sdk.messaging.graphql.DeleteMessageMutation
import com.nabla.sdk.messaging.graphql.SendMessageMutation
import com.nabla.sdk.messaging.graphql.SetTypingMutation
import com.nabla.sdk.messaging.graphql.fragment.ConversationActivityFragment
import com.nabla.sdk.messaging.graphql.fragment.ConversationItemsPageFragment
import com.nabla.sdk.messaging.graphql.fragment.ConversationPreviewFragment
import com.nabla.sdk.messaging.graphql.fragment.MessageContentFragment
import com.nabla.sdk.messaging.graphql.fragment.MessageFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import com.nabla.sdk.messaging.core.domain.entity.Message as DomainEntityMessage

internal class GqlConversationContentDataSource(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
    private val exceptionMapper: NablaExceptionMapper,
    private val localConversationDataSource: LocalConversationDataSource,
) {

    private val conversationEventsFlowMap = mutableMapOf<ConversationId.Remote, Flow<Unit>>()

    internal fun conversationEventsFlow(conversationId: ConversationId.Remote): Flow<Unit> {
        return synchronized(this) {
            return@synchronized conversationEventsFlowMap.getOrPut(conversationId) {
                createConversationEventsFlow(conversationId)
            }
        }
    }

    private fun createConversationEventsFlow(conversationId: ConversationId.Remote): Flow<Unit> {
        val subscription = ConversationEventsSubscription(conversationId.remoteId)
        return apolloClient.subscription(subscription)
            .toFlow()
            .onEach { response ->
                response.errors?.forEach {
                    logger.error(domain = GQL_DOMAIN, message = "error received in $subscription: ${it.message}")
                }
                response.data?.conversation?.event?.let { putInsertEventToCache(it) }
            }
            .retryOnNetworkErrorAndShareIn(coroutineScope)
            .filterIsInstance()
    }

    private suspend fun putInsertEventToCache(
        event: ConversationEventsSubscription.Event,
    ) {
        logger.debug(
            domain = GQL_DOMAIN,
            message = "Event ${event.__typename}"
        )
        event.onMessageCreatedEvent?.message?.messageFragment?.let { messageFragment ->
            insertMessageToConversationCache(messageFragment)
        }
        event.onConversationActivityCreated?.activity?.conversationActivityFragment?.let { conversationActivityFragment ->
            insertConversationActivityToConversationCache(conversationActivityFragment)
        }
    }

    private suspend fun insertMessageToConversationCache(
        newMessageFragment: MessageFragment,
    ) {
        val query = conversationItemsQuery(
            ConversationId.Remote(
                remoteId = newMessageFragment.messageSummaryFragment.conversation.id
            )
        )
        val newItem = ConversationItemsPageFragment.Data(
            Message.type.name,
            null,
            newMessageFragment
        )
        insertConversationItemToConversationCache(query, newItem) { messageFragment?.messageSummaryFragment?.id }
    }

    private suspend fun insertConversationActivityToConversationCache(
        newConversationActivityFragment: ConversationActivityFragment,
    ) {
        val query = conversationItemsQuery(
            ConversationId.Remote(
                remoteId = newConversationActivityFragment.conversation.id
            )
        )
        val newItem = ConversationItemsPageFragment.Data(
            ConversationActivity.type.name,
            newConversationActivityFragment,
            null
        )
        insertConversationItemToConversationCache(query, newItem) { conversationActivityFragment?.id }
    }

    private suspend fun insertConversationItemToConversationCache(
        query: ConversationItemsQuery,
        newItem: ConversationItemsPageFragment.Data,
        itemIdGetter: ConversationItemsPageFragment.Data.() -> Uuid?,
    ) {
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val items = cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.data
            val isAlreadyInCache = items.any { it?.itemIdGetter() == newItem.itemIdGetter() }

            if (isAlreadyInCache) return@updateCache CacheUpdateOperation.Ignore()

            val mergedQueryData = cachedQueryData.modify(listOf(newItem) + items)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    internal suspend fun findMessageInConversationCache(
        conversationId: ConversationId.Remote,
        messageId: MessageId,
    ): DomainEntityMessage? {
        val cacheData = apolloClient.readFromCache(conversationItemsQuery(conversationId))

        return cacheData?.conversation?.conversation?.conversationItemsPageFragment?.items?.data
            ?.firstOrNull { item -> item?.messageFragment?.messageSummaryFragment?.id == messageId.remoteId }
            ?.messageFragment
            ?.let {
                mapper.mapToMessage(
                    it.messageSummaryFragment,
                    SendStatus.Sent,
                    it.replyTo?.messageSummaryFragment,
                )
            }
    }

    suspend fun loadMoreConversationItemsInCache(conversationId: ConversationId.Remote) {
        apolloClient.updateCache(conversationItemsQuery(conversationId)) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val nextCursor = requireNotNull(cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.nextCursor)
            val updatedQuery = conversationItemsQuery(conversationId, nextCursor)

            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataOrThrowOnError
            val mergedData =
                (cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.data + freshQueryData.conversation.conversation.conversationItemsPageFragment.items.data)
                    .distinctBy { it?.messageFragment?.messageSummaryFragment?.id }
            val mergedQueryData = freshQueryData.modify(mergedData)
            return@updateCache CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    @OptIn(FlowPreview::class)
    fun watchConversationItems(conversationId: ConversationId.Remote): Flow<Response<PaginatedList<ConversationItem>>> {
        val dataFlow = apolloClient.query(conversationItemsQuery(conversationId))
            .watchAsCachedResponse(exceptionMapper)
            .map { response ->
                val page = response.data.conversation.conversation.conversationItemsPageFragment.items
                val items = page.data.mapNotNull { pageData ->
                    pageData?.messageFragment?.let {
                        return@mapNotNull mapper.mapToMessage(
                            it.messageSummaryFragment,
                            SendStatus.Sent,
                            it.replyTo?.messageSummaryFragment,
                        )
                    }
                    pageData?.conversationActivityFragment?.let {
                        return@mapNotNull mapper.mapToConversationActivity(it)
                    }
                }

                return@map Response(
                    isDataFresh = response.isDataFresh,
                    refreshingState = response.refreshingState,
                    data = PaginatedList(
                        items = items,
                        hasMore = page.hasMore,
                    ),
                )
            }
        return flowOf(
            conversationEventsFlow(conversationId),
            dataFlow,
        ).flattenMerge().filterIsInstance()
    }

    suspend fun sendMessage(
        conversationId: ConversationId.Remote,
        input: SendMessageInput,
    ) {
        val mutation = SendMessageMutation(
            conversationId = conversationId.remoteId,
            input = input,
        )
        apolloClient.mutation(mutation).execute().dataOrThrowOnError
    }

    suspend fun deleteMessage(conversationId: ConversationId, remoteMessageId: MessageId.Remote) {
        val mutation = DeleteMessageMutation(remoteMessageId.remoteId)

        val conversationRemoteId = when (conversationId) {
            is ConversationId.Local -> {
                localConversationDataSource.watch(conversationId).first()
                    .creationState
                    .let { it.requireCreated("Deleting Remote message $remoteMessageId in a not Created conversation $it") }
                    .remoteId
            }
            is ConversationId.Remote -> conversationId
        }

        val cachedConversationFragment = apolloClient.readFromCache(ConversationQuery(conversationRemoteId.remoteId))
            ?.conversation?.conversation?.conversationFragment

        val optimisticData = DeleteMessageMutation.Data(
            deleteMessage = DeleteMessageMutation.DeleteMessage(
                message = DeleteMessageMutation.Message(
                    id = remoteMessageId.remoteId,
                    content = DeleteMessageMutation.Content(
                        __typename = DeletedMessageContent.type.name,
                        messageContentFragment = MessageContentFragment(
                            __typename = DeletedMessageContent.type.name,
                            onTextMessageContent = null,
                            onImageMessageContent = null,
                            onVideoMessageContent = null,
                            onDocumentMessageContent = null,
                            onAudioMessageContent = null,
                            onLivekitRoomMessageContent = null,
                            onDeletedMessageContent = MessageContentFragment.OnDeletedMessageContent(
                                empty = EmptyObject.EMPTY
                            ),
                        )
                    ),
                    conversation = DeleteMessageMutation.Conversation(
                        __typename = Conversation.type.name,
                        conversationPreviewFragment = ConversationPreviewFragment(
                            id = conversationRemoteId.remoteId,
                            updatedAt = Clock.System.now(),
                            inboxPreviewTitle = cachedConversationFragment?.inboxPreviewTitle ?: "",
                            lastMessagePreview = cachedConversationFragment?.lastMessagePreview,
                            unreadMessageCount = cachedConversationFragment?.unreadMessageCount ?: 0,
                        )
                    )
                )
            )
        )

        apolloClient.mutation(mutation)
            .optimisticUpdates(optimisticData)
            .execute()
            .dataOrThrowOnError
    }

    suspend fun setTyping(conversationId: ConversationId.Remote, typing: Boolean) {
        apolloClient.mutation(SetTypingMutation(conversationId.remoteId, typing)).execute()
    }

    companion object {
        @VisibleForTesting
        internal fun conversationItemsQuery(id: ConversationId.Remote, cursorPage: String? = null) =
            ConversationItemsQuery(id.remoteId, OpaqueCursorPage(cursor = Optional.presentIfNotNull(cursorPage), numberOfItems = Optional.Present(50)))
    }
}
