package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.kotlin.KotlinExt.sharedSingleIn
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.MessageNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ConversationContentRepositoryImpl(
    private val repoScope: CoroutineScope,
    private val localMessageDataSource: LocalMessageDataSource,
    private val localConversationDataSource: LocalConversationDataSource,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
    private val sendMessageOrchestrator: SendMessageOrchestrator,
    private val messageMapper: MessageMapper,
    private val isVideoCallModuleActive: Boolean,
) : ConversationContentRepository {

    private val loadMoreConversationMessagesSharedSingleLock = Mutex()
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<ConversationId, SharedSingle<Unit, Result<Unit>>>()

    override fun watchConversationItems(conversationId: ConversationId): Flow<Response<PaginatedList<ConversationItem>>> {
        val typedFlow: Flow<Response<PaginatedList<ConversationItem>>> = when (conversationId) {
            is ConversationId.Local -> {
                localConversationDataSource.watch(conversationId).flatMapLatest { localConversation ->
                    when (localConversation.creationState) {
                        LocalConversation.CreationState.Creating,
                        is LocalConversation.CreationState.ErrorCreating,
                        LocalConversation.CreationState.ToBeCreated,
                        -> {
                            localMessageDataSource.watchLocalMessages(conversationId)
                                .map {
                                    Response(
                                        isDataFresh = true,
                                        refreshingState = RefreshingState.Refreshed,
                                        data = PaginatedList(
                                            items = it.toList(),
                                            hasMore = false,
                                        ),
                                    )
                                }
                        }
                        is LocalConversation.CreationState.Created -> {
                            watchRemoteConversationItems(localConversation.creationState.remoteId)
                        }
                    }
                }
            }
            is ConversationId.Remote -> watchRemoteConversationItems(conversationId)
        }
        return typedFlow.map { filterLivekitMessagesIfNeeded(it) }.filterIsInstance()
    }

    private fun filterLivekitMessagesIfNeeded(
        paginatedConversationResponse: Response<PaginatedList<ConversationItem>>,
    ): Response<PaginatedList<ConversationItem>> {
        return paginatedConversationResponse.copy(
            data = paginatedConversationResponse.data.copy(
                items = paginatedConversationResponse.data.items.filter {
                    it !is Message.VideoCallRoom || isVideoCallModuleActive
                },
            ),
        )
    }

    private fun watchRemoteConversationItems(conversationId: ConversationId.Remote): Flow<Response<PaginatedList<ConversationItem>>> {
        val gqlConversationAndMessagesFlow = gqlConversationContentDataSource.watchConversationItems(conversationId)
        val localMessagesFlow = localMessageDataSource.watchLocalMessages(conversationId)
        val messageFlow = gqlConversationAndMessagesFlow.combine(localMessagesFlow) { gqlConversationAndMessagesResponse, localMessages ->
            return@combine combineGqlAndLocalInfo(localMessages, gqlConversationAndMessagesResponse)
        }
        return messageFlow
    }

    private fun combineGqlAndLocalInfo(
        localMessages: Collection<Message>,
        gqlConversationAndMessagesResponse: Response<PaginatedList<ConversationItem>>,
    ): Response<PaginatedList<ConversationItem>> {
        val (gqlConversationItemMessages, gqlConversationItemNotMessages) = gqlConversationAndMessagesResponse.data.items.partition { conversationItem ->
            conversationItem is Message
        }
        val gqlMessages = gqlConversationItemMessages.map { it as Message }
        val (localMessagesToMerge, localMessagesToAdd) = localMessages.partition { localMessage ->
            localMessage.baseMessage.id.stableId in gqlMessages.map { it.baseMessage.id.stableId }
        }.let { Pair(it.first.associateBy { it.baseMessage.id.stableId }, it.second) }
        val mergedMessages = gqlMessages.map { gqlMessage ->
            val mergeResult = localMessagesToMerge[gqlMessage.baseMessage.id.stableId]?.let { localMessage ->
                mergeMessage(gqlMessage, localMessage)
            }
            mergeResult ?: gqlMessage
        }
        val allMessages = (mergedMessages + localMessagesToAdd)
        val allItems: List<ConversationItem> = (allMessages + gqlConversationItemNotMessages).sortedByDescending { conversationItem ->
            conversationItem.createdAt
        }

        return Response(
            isDataFresh = gqlConversationAndMessagesResponse.isDataFresh,
            refreshingState = gqlConversationAndMessagesResponse.refreshingState,
            data = gqlConversationAndMessagesResponse.data.copy(
                items = allItems,
            ),
        )
    }

    private fun mergeMessage(gqlMessage: Message, localMessage: Message): Message? {
        if (gqlMessage is Message.Media.Image && localMessage is Message.Media.Image) {
            val gqlMessageMediaSource = gqlMessage.mediaSource
            if (gqlMessageMediaSource !is FileSource.Uploaded) return null
            val localMessageMediaSource = localMessage.mediaSource
            if (localMessageMediaSource !is FileSource.Local) return null
            return gqlMessage.modify(
                mediaSource = FileSource.Uploaded(
                    fileLocal = localMessageMediaSource.fileLocal,
                    fileUpload = gqlMessageMediaSource.fileUpload,
                ),
            )
        }
        return null
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        when (conversationId) {
            is ConversationId.Local -> {
                loadMoreRemoteMessages(
                    localConversationDataSource.waitConversationCreated(conversationId),
                )
            }
            is ConversationId.Remote -> loadMoreRemoteMessages(conversationId)
        }
    }

    private suspend fun loadMoreRemoteMessages(conversationId: ConversationId.Remote) {
        val loadMoreConversationMessagesSharedSingle = loadMoreConversationMessagesSharedSingleLock.withLock {
            loadMoreConversationMessagesSharedSingleMap.getOrPut(conversationId) {
                sharedSingleIn(repoScope) {
                    gqlConversationContentDataSource.loadMoreConversationItemsInCache(conversationId)
                }
            }
        }
        loadMoreConversationMessagesSharedSingle.await()
    }

    override suspend fun sendMessage(
        input: MessageInput,
        conversationId: ConversationId,
        replyTo: MessageId.Remote?,
    ): MessageId.Local {
        val message = messageMapper.messageInputToNewMessage(input, conversationId as? ConversationId.Remote, replyTo)
        return sendMessageOrchestrator.sendMessage(message, conversationId)
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        val localMessage = localMessageDataSource.getMessage(conversationId, localMessageId)
        if (localMessage != null) {
            sendMessageOrchestrator.sendMessage(
                localMessage,
                conversationId,
            )
        } else {
            throw MessageNotFoundException(conversationId, localMessageId)
        }
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        when (conversationId) {
            is ConversationId.Local -> { /* no-op */
            }
            is ConversationId.Remote -> gqlConversationContentDataSource.setTyping(conversationId, isTyping)
        }
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId) {
        when (messageId) {
            is MessageId.Local -> localMessageDataSource.removeMessage(conversationId, messageId)
            is MessageId.Remote -> gqlConversationContentDataSource.deleteMessage(conversationId, messageId)
        }
    }
}
