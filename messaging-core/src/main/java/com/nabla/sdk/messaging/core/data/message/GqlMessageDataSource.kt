package com.nabla.sdk.messaging.core.data.message

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.optimisticUpdates
import com.apollographql.apollo3.cache.normalized.watch
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedConversationWithMessages
import com.nabla.sdk.core.kotlin.shareInWithMaterializedErrors
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationQuery
import com.nabla.sdk.graphql.DeleteMessageMutation
import com.nabla.sdk.graphql.SendMessageMutation
import com.nabla.sdk.graphql.SetTypingMutation
import com.nabla.sdk.graphql.fragment.ConversationMessagesPageFragment
import com.nabla.sdk.graphql.fragment.DeletedMessageContentFragment
import com.nabla.sdk.graphql.fragment.MessageContentFragment
import com.nabla.sdk.graphql.fragment.MessageFragment
import com.nabla.sdk.graphql.type.DeleteMessageOutput
import com.nabla.sdk.graphql.type.DeletedMessageContent
import com.nabla.sdk.graphql.type.EmptyObject
import com.nabla.sdk.graphql.type.MessageContent
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.graphql.type.SendDocumentMessageInput
import com.nabla.sdk.graphql.type.SendImageMessageInput
import com.nabla.sdk.graphql.type.SendMessageContentInput
import com.nabla.sdk.graphql.type.SendTextMessageInput
import com.nabla.sdk.graphql.type.UploadInput
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.apollo.GqlTypeHelper.modify
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class GqlMessageDataSource(
    private val logger: Logger,
    private val coroutineScope: CoroutineScope,
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {

    private val conversationEventsFlowMap = mutableMapOf<ConversationId, Flow<Unit>>()

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
            .map { it.dataAssertNoErrors }
            .onEach {
                it.conversation?.event?.onMessageCreatedEvent?.let {
                    logger.debug("onMessageCreatedEvent")
                }
                it.conversation?.event?.onMessageUpdatedEvent?.let {
                    logger.debug("onMessageUpdatedEvent")
                }
                it.conversation?.event?.onMessageCreatedEvent?.message?.messageFragment?.let { messageFragment ->
                    insertMessageToConversationCache(messageFragment)
                }
            }.shareInWithMaterializedErrors(
                scope = coroutineScope,
                replay = 0,
                started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)
            ).filterIsInstance()
    }

    private suspend fun insertMessageToConversationCache(
        message: MessageFragment,
    ) {
        val query = firstMessagePageQuery(message.conversation.id.toConversationId())
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null) return@updateCache CacheUpdateOperation.Ignore()
            val newItem = ConversationMessagesPageFragment.Data(
                message.__typename,
                message
            )
            val mergedItemsData = listOf(newItem) + cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.data
            val mergedQueryData = cachedQueryData.modify(mergedItemsData)
            CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    suspend fun loadMoreConversationMessagesInCache(conversationId: ConversationId) {
        val query = firstMessagePageQuery(conversationId)
        apolloClient.updateCache(query) { cachedQueryData ->
            if (cachedQueryData == null || !cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val nextCursor =
                requireNotNull(cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.nextCursor)
            val updatedQuery = query.copy(
                pageInfo = OpaqueCursorPage(
                    cursor = Optional.presentIfNotNull(nextCursor.toString()) // TODO : Schema update is probably needed here
                )
            )
            val freshQueryData = apolloClient.query(updatedQuery)
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataAssertNoErrors
            val mergedData =
                (cachedQueryData.conversation.conversation.conversationMessagesPageFragment.items.data + freshQueryData.conversation.conversation.conversationMessagesPageFragment.items.data).distinctBy { it?.messageFragment?.id }
            val mergedQueryData = freshQueryData.modify(mergedData)
            return@updateCache CacheUpdateOperation.Write(mergedQueryData)
        }
    }

    fun watchRemoteConversationWithMessages(conversationId: ConversationId): Flow<PaginatedConversationWithMessages> {
        val query = firstMessagePageQuery(conversationId)
        val dataFlow = apolloClient.query(query)
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch(fetchThrows = true, refetchThrows = true)
            .map { response -> requireNotNull(response.data) }
            .map { queryData ->
                val page =
                    queryData.conversation.conversation.conversationMessagesPageFragment.items
                val items = page.data.mapNotNull { it?.messageFragment }.map {
                    mapper.mapToMessage(it, SendStatus.Sent)
                }
                return@map PaginatedConversationWithMessages(
                    conversationWithMessages = ConversationWithMessages(
                        conversation = mapper.mapToConversation(queryData.conversation.conversation.conversationFragment),
                        messages = items,
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
    ): Message {
        return sendMediaMessage(conversationId, clientId) {
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
    ): Message {
        return sendMediaMessage(conversationId, clientId) {
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
    ): Message {
        val input = inputFactoryBlock()
        val mutation = SendMessageMutation(conversationId.value, input, clientId)

        return mapper.mapToMessage(
            apolloClient.mutation(mutation)
                .execute()
                .dataAssertNoErrors
                .sendMessage
                .message
                .messageFragment,
            SendStatus.Sent
        )
    }

    suspend fun sendTextMessage(conversationId: ConversationId, clientId: Uuid, text: String): Message {
        val input = SendMessageContentInput(
            textInput = Optional.presentIfNotNull(
                SendTextMessageInput(
                    text = text
                )
            )
        )
        val mutation = SendMessageMutation(conversationId.value, input, clientId)

        return mapper.mapToMessage(
            apolloClient.mutation(mutation)
                .execute()
                .dataAssertNoErrors
                .sendMessage
                .message
                .messageFragment,
            SendStatus.Sent
        )
    }

    suspend fun deleteMessage(remoteMessageId: Uuid) {
        val mutation = DeleteMessageMutation(remoteMessageId)
        val optimisticData = DeleteMessageMutation.Data(
            deleteMessage = DeleteMessageMutation.DeleteMessage(
                message = DeleteMessageMutation.Message(
                    content = DeleteMessageMutation.Content(
                        __typename = DeleteMessageOutput.type.name, // TODO : Double check expected type with mutation real execution
                        messageContentFragment = MessageContentFragment(
                            __typename = MessageContent.type.name,
                            onTextMessageContent = null,
                            onImageMessageContent = null,
                            onDocumentMessageContent = null,
                            onDeletedMessageContent = MessageContentFragment.OnDeletedMessageContent(
                                __typename = DeletedMessageContent.type.name,
                                deletedMessageContentFragment = DeletedMessageContentFragment(
                                    EmptyObject.EMPTY
                                )
                            )
                        )
                    )
                )
            )
        )

        apolloClient.mutation(mutation)
            .optimisticUpdates(optimisticData)
            .execute()
    }

    private fun firstMessagePageQuery(id: ConversationId) =
        ConversationQuery(id.value, OpaqueCursorPage(cursor = Optional.Absent))

    suspend fun setTyping(conversationId: ConversationId, typing: Boolean) {
        apolloClient.mutation(SetTypingMutation(conversationId.value, typing)).execute()
    }
}
