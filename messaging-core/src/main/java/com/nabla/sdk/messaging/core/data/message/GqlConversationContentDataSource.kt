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
import com.nabla.sdk.core.data.apollo.readFromCache
import com.nabla.sdk.core.data.apollo.retryOnNetworkErrorAndShareIn
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.boundary.Logger
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
import com.nabla.sdk.graphql.type.SendDocumentMessageInput
import com.nabla.sdk.graphql.type.SendImageMessageInput
import com.nabla.sdk.graphql.type.SendMessageContentInput
import com.nabla.sdk.graphql.type.SendTextMessageInput
import com.nabla.sdk.graphql.type.UploadInput
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock

internal class GqlConversationContentDataSource(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {

    private val conversationEventsFlowMap = mutableMapOf<ConversationId, Flow<Unit>>()

    private val tag: String = Logger.asSdkTag("message")

    private fun conversationEventsFlow(conversationId: ConversationId): Flow<Unit> {
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
                logger.debug("Event $it", tag = tag)
                it.dataAssertNoErrors.conversation?.event?.onMessageCreatedEvent?.message?.messageFragment?.let { messageFragment ->
                    insertMessageToConversationCache(messageFragment)
                }
                it.dataAssertNoErrors.conversation?.event?.onConversationActivityCreated?.activity?.conversationActivityFragment?.let { conversationActivityFragment ->
                    insertConversationActivityToConversationCache(conversationActivityFragment)
                }
            }.filterIsInstance()
    }

    private suspend fun insertMessageToConversationCache(
        messageFragment: MessageFragment,
    ) {
        val query = firstItemsPageQuery(messageFragment.conversation.id.toConversationId())
        val newItem = ConversationItemsPageFragment.Data(
            Message.type.name,
            null,
            messageFragment
        )
        insertConversationItemToConversationCache(query, newItem)
    }

    private suspend fun insertConversationActivityToConversationCache(
        conversationActivityFragment: ConversationActivityFragment
    ) {
        val query = firstItemsPageQuery(conversationActivityFragment.conversation.id.toConversationId())
        val newItem = ConversationItemsPageFragment.Data(
            ConversationActivity.type.name,
            conversationActivityFragment,
            null
        )
        insertConversationItemToConversationCache(query, newItem)
    }

    private suspend fun insertConversationItemToConversationCache(
        query: ConversationItemsQuery,
        newItem: ConversationItemsPageFragment.Data
    ) {
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val mergedItemsData =
                listOf(newItem) + cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.data
            val mergedQueryData = cachedQueryData.modify(mergedItemsData)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun loadMoreConversationItemsInCache(conversationId: ConversationId) {
        val query = firstItemsPageQuery(conversationId)
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val nextCursor =
                requireNotNull(cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.nextCursor)
            val updatedQuery = query.copy(
                pageInfo = OpaqueCursorPage(
                    cursor = Optional.presentIfNotNull(nextCursor)
                )
            )
            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataAssertNoErrors
            val mergedData =
                (cachedQueryData.conversation.conversation.conversationItemsPageFragment.items.data + freshQueryData.conversation.conversation.conversationItemsPageFragment.items.data).distinctBy { it?.messageFragment?.id }
            val mergedQueryData = freshQueryData.modify(mergedData)
            return@updateCache CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    fun watchRemoteConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems> {
        val query = firstItemsPageQuery(conversationId)
        val dataFlow = apolloClient.query(query)
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true)
            .map { response -> requireNotNull(response.data) }
            .map { queryData ->
                val page =
                    queryData.conversation.conversation.conversationItemsPageFragment.items
                val items = page.data.mapNotNull {
                    it?.messageFragment?.let {
                        return@mapNotNull mapper.mapToMessage(it, SendStatus.Sent)
                    }
                    it?.conversationActivityFragment?.let {
                        return@mapNotNull mapper.mapToConversationActivity(it)
                    }
                }
                return@map PaginatedConversationItems(
                    conversationItems = ConversationItems(
                        conversationId = queryData.conversation.conversation.id.toConversationId(),
                        items = items,
                    ),
                    hasMore = page.hasMore,
                )
            }
        return flowOf(
            conversationEventsFlow(conversationId),
            dataFlow
        ).flattenMerge().filterIsInstance()
    }

    suspend fun sendDocumentMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid
    ) {
        sendMediaMessage(conversationId, clientId) {
            SendMessageContentInput(
                documentInput = Optional.presentIfNotNull(
                    SendDocumentMessageInput(
                        UploadInput(fileUploadId)
                    )
                ),
            )
        }
    }

    suspend fun sendImageMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        fileUploadId: Uuid
    ) {
        sendMediaMessage(conversationId, clientId) {
            SendMessageContentInput(
                imageInput = Optional.presentIfNotNull(
                    SendImageMessageInput(
                        UploadInput(fileUploadId)
                    )
                ),
            )
        }
    }

    private suspend fun sendMediaMessage(
        conversationId: ConversationId,
        clientId: Uuid,
        inputFactoryBlock: () -> SendMessageContentInput
    ) {
        val input = inputFactoryBlock()
        val mutation = SendMessageMutation(conversationId.value, input, clientId)
        apolloClient.mutation(mutation).execute().dataAssertNoErrors
    }

    suspend fun sendTextMessage(conversationId: ConversationId, clientId: Uuid, text: String) {
        val input = SendMessageContentInput(
            textInput = Optional.presentIfNotNull(
                SendTextMessageInput(
                    text = text
                )
            )
        )
        val mutation = SendMessageMutation(conversationId.value, input, clientId)

        apolloClient.mutation(mutation).execute().dataAssertNoErrors
    }

    suspend fun deleteMessage(conversationId: ConversationId, remoteMessageId: MessageId.Remote) {
        val mutation = DeleteMessageMutation(remoteMessageId.remoteId)

        val cachedConversationFragment = apolloClient.readFromCache(ConversationQuery(conversationId.value))
            ?.conversation?.conversation?.conversationFragment

        val optimisticData = DeleteMessageMutation.Data(
            deleteMessage = DeleteMessageMutation.DeleteMessage(
                message = DeleteMessageMutation.Message(
                    content = DeleteMessageMutation.Content(
                        __typename = DeletedMessageContent.type.name,
                        messageContentFragment = MessageContentFragment(
                            __typename = DeletedMessageContent.type.name,
                            onTextMessageContent = null,
                            onImageMessageContent = null,
                            onDocumentMessageContent = null,
                            onDeletedMessageContent = MessageContentFragment.OnDeletedMessageContent(
                                empty = EmptyObject.EMPTY
                            )
                        )
                    ),
                    conversation = DeleteMessageMutation.Conversation(
                        __typename = Conversation.type.name,
                        conversationPreviewFragment = ConversationPreviewFragment(
                            id = conversationId.value,
                            updatedAt = Clock.System.now(),
                            inboxPreviewTitle = cachedConversationFragment?.inboxPreviewTitle ?: "",
                            lastMessagePreview = cachedConversationFragment?.lastMessagePreview
                        )
                    )
                )
            )
        )

        apolloClient.mutation(mutation)
            .optimisticUpdates(optimisticData)
            .execute()
            .dataAssertNoErrors
    }

    @VisibleForTesting
    internal fun firstItemsPageQuery(id: ConversationId) =
        ConversationItemsQuery(id.value, OpaqueCursorPage(cursor = Optional.Absent))

    suspend fun setTyping(conversationId: ConversationId, typing: Boolean) {
        apolloClient.mutation(SetTypingMutation(conversationId.value, typing)).execute()
    }
}
