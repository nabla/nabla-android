package com.nabla.sdk.messaging.core.data.message

import androidx.annotation.VisibleForTesting
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.apollographql.apollo3.cache.normalized.watch
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.readFromCache
import com.nabla.sdk.core.data.apollo.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.GQL_DOMAIN
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationItemsQuery
import com.nabla.sdk.graphql.ConversationQuery
import com.nabla.sdk.graphql.DeleteMessageMutation
import com.nabla.sdk.graphql.SendMessageMutation
import com.nabla.sdk.graphql.SetTypingMutation
import com.nabla.sdk.graphql.fragment.ConversationActivityFragment
import com.nabla.sdk.graphql.fragment.ConversationItemsPageFragment
import com.nabla.sdk.graphql.fragment.ConversationPreviewFragment
import com.nabla.sdk.graphql.fragment.MessageContentFragment
import com.nabla.sdk.graphql.fragment.MessageFragment
import com.nabla.sdk.graphql.type.Conversation
import com.nabla.sdk.graphql.type.ConversationActivity
import com.nabla.sdk.graphql.type.DeletedMessageContent
import com.nabla.sdk.graphql.type.EmptyObject
import com.nabla.sdk.graphql.type.Message
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.graphql.type.SendAudioMessageInput
import com.nabla.sdk.graphql.type.SendDocumentMessageInput
import com.nabla.sdk.graphql.type.SendImageMessageInput
import com.nabla.sdk.graphql.type.SendMessageContentInput
import com.nabla.sdk.graphql.type.SendTextMessageInput
import com.nabla.sdk.graphql.type.SendVideoMessageInput
import com.nabla.sdk.graphql.type.UploadInput
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
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
) {

    private val conversationEventsFlowMap = mutableMapOf<ConversationId, Flow<Unit>>()

    internal fun conversationEventsFlow(conversationId: ConversationId): Flow<Unit> {
        return synchronized(this) {
            return@synchronized conversationEventsFlowMap.getOrPut(conversationId) {
                createConversationEventsFlow(conversationId)
            }
        }
    }

    private fun createConversationEventsFlow(conversationId: ConversationId): Flow<Unit> {
        return apolloClient.subscription(ConversationEventsSubscription(conversationId.value))
            .toFlow()
            .retryOnNetworkErrorAndShareIn(coroutineScope)
            .onEach {
                logger.debug(domain = GQL_DOMAIN, message = "Event $it")
                it.dataOrThrowOnError.conversation?.event?.onMessageCreatedEvent?.message?.messageFragment?.let { messageFragment ->
                    insertMessageToConversationCache(messageFragment)
                }
                it.dataOrThrowOnError.conversation?.event?.onConversationActivityCreated?.activity?.conversationActivityFragment?.let { conversationActivityFragment ->
                    insertConversationActivityToConversationCache(conversationActivityFragment)
                }
            }.filterIsInstance()
    }

    private suspend fun insertMessageToConversationCache(
        newMessageFragment: MessageFragment,
    ) {
        val query = firstItemsPageQuery(newMessageFragment.messageSummaryFragment.conversation.id.toConversationId())
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
        val query = firstItemsPageQuery(newConversationActivityFragment.conversation.id.toConversationId())
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
        conversationId: ConversationId,
        messageId: MessageId,
    ): DomainEntityMessage? {
        val cacheData = apolloClient.readFromCache(firstItemsPageQuery(conversationId))

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

    suspend fun loadMoreConversationItemsInCache(conversationId: ConversationId) {
        val query = firstItemsPageQuery(conversationId)
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val nextCursor = requireNotNull(cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.nextCursor)
            val updatedQuery = query.copy(
                pageInfo = OpaqueCursorPage(
                    cursor = Optional.presentIfNotNull(nextCursor)
                )
            )
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
    fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems> {
        val query = firstItemsPageQuery(conversationId)
        val dataFlow = apolloClient.query(query)
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true)
            .map { response -> requireNotNull(response.data) }
            .map { queryData ->
                val page = queryData.conversation.conversation.conversationItemsPageFragment.items
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
                PaginatedConversationItems(
                    conversationItems = ConversationItems(
                        conversationId = queryData.conversation.conversation.id.toConversationId(),
                        items = items,
                    ),
                    hasMore = page.hasMore,
                )
            }
        return flowOf(
            conversationEventsFlow(conversationId),
            dataFlow,
        ).flattenMerge().filterIsInstance()
    }

    suspend fun sendDocumentMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid,
        replyToMessageId: Uuid?,
    ) {
        sendMediaMessage(conversationId, clientId, replyToMessageId = replyToMessageId) {
            SendMessageContentInput(
                documentInput = Optional.presentIfNotNull(
                    SendDocumentMessageInput(
                        UploadInput(fileUploadId),
                    ),
                ),
            )
        }
    }

    suspend fun sendImageMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid,
        replyToMessageId: Uuid?,
    ) {
        sendMediaMessage(conversationId, clientId, replyToMessageId = replyToMessageId) {
            SendMessageContentInput(
                imageInput = Optional.presentIfNotNull(
                    SendImageMessageInput(
                        UploadInput(fileUploadId),
                    ),
                ),
            )
        }
    }

    suspend fun sendVideoMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid,
        replyToMessageId: Uuid?,
    ) {
        sendMediaMessage(conversationId, clientId, replyToMessageId = replyToMessageId) {
            SendMessageContentInput(
                videoInput = Optional.presentIfNotNull(
                    SendVideoMessageInput(
                        UploadInput(fileUploadId),
                    ),
                ),
            )
        }
    }

    suspend fun sendAudioMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid,
        replyToMessageId: Uuid?,
    ) {
        sendMediaMessage(conversationId, clientId, replyToMessageId = replyToMessageId) {
            SendMessageContentInput(
                audioInput = Optional.presentIfNotNull(
                    SendAudioMessageInput(
                        UploadInput(fileUploadId),
                    ),
                ),
            )
        }
    }

    private suspend fun sendMediaMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        replyToMessageId: Uuid?,
        inputFactoryBlock: () -> SendMessageContentInput,
    ) {
        val input = inputFactoryBlock()
        val mutation = SendMessageMutation(
            conversationId = conversationId.value,
            content = input,
            clientId = clientId,
            replyToMessageId = Optional.presentIfNotNull(replyToMessageId),
        )
        apolloClient.mutation(mutation).execute().dataOrThrowOnError
    }

    suspend fun sendTextMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        text: String,
        replyToMessageId: Uuid?,
    ) {
        val input = SendMessageContentInput(
            textInput = Optional.presentIfNotNull(SendTextMessageInput(text = text)),
        )
        val mutation = SendMessageMutation(
            conversationId = conversationId.value,
            content = input,
            clientId = clientId,
            replyToMessageId = Optional.presentIfNotNull(replyToMessageId),
        )

        apolloClient.mutation(mutation).execute().dataOrThrowOnError
    }

    suspend fun deleteMessage(conversationId: ConversationId, remoteMessageId: MessageId.Remote) {
        val mutation = DeleteMessageMutation(remoteMessageId.remoteId)

        val cachedConversationFragment = apolloClient.readFromCache(ConversationQuery(conversationId.value))
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
                            onDeletedMessageContent = MessageContentFragment.OnDeletedMessageContent(
                                empty = EmptyObject.EMPTY
                            ),
                        )
                    ),
                    conversation = DeleteMessageMutation.Conversation(
                        __typename = Conversation.type.name,
                        conversationPreviewFragment = ConversationPreviewFragment(
                            id = conversationId.value,
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

    @VisibleForTesting
    internal fun firstItemsPageQuery(id: ConversationId) =
        ConversationItemsQuery(id.value, OpaqueCursorPage(cursor = Optional.Absent))

    suspend fun setTyping(conversationId: ConversationId, typing: Boolean) {
        apolloClient.mutation(SetTypingMutation(conversationId.value, typing)).execute()
    }
}
