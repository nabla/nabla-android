package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.GqlMessageDataSource
import com.nabla.sdk.messaging.core.data.apollo.MessagingGqlOperationHelper
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class MessageRepositoryImpl(
    private val repoScope: CoroutineScope,
    private val gqlOperationHelper: MessagingGqlOperationHelper,
    private val localMessageDataSource: LocalMessageDataSource,
    private val gqlMessageDataSource: GqlMessageDataSource,
    private val fileUploadRepository: FileUploadRepository,
) : MessageRepository {

    private val loadMoreConversationMessagesSharedSingleLock = Mutex()
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<ConversationId, SharedSingle<Unit>>()

    override fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
        val gqlConversationAndMessagesFlow = gqlMessageDataSource.watchRemoteConversationWithMessages(conversationId)
        val localMessagesFlow = localMessageDataSource.watchLocalMessages(conversationId)
        val messageFlow = gqlConversationAndMessagesFlow.combine(localMessagesFlow) { gqlConversationAndMessages, localMessages ->
            return@combine combineGqlAndLocalInfo(localMessages, gqlConversationAndMessages)
        }
        return messageFlow
    }

    private fun combineGqlAndLocalInfo(
        localMessages: Collection<Message>,
        gqlConversationAndMessages: ConversationWithMessages
    ): ConversationWithMessages {
        val (localMessagesToMerge, localMessagesToAdd) = localMessages.partition {
            it.message.id.stableId in gqlConversationAndMessages.messages.items.map { it.message.id.stableId }
        }.let { Pair(it.first.associateBy { it.message.id.stableId }, it.second) }
        val mergedMessages = gqlConversationAndMessages.messages.items.map {
            mergeMessage(it, localMessagesToMerge.getValue(it.message.id.stableId)) ?: it
        }
        val allMessages = (mergedMessages + localMessagesToAdd).sortedBy { it.message.sentAt }
        return gqlConversationAndMessages.copy(
            messages = gqlConversationAndMessages.messages.copy(
                items = allMessages
            )
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
                    fileUpload = gqlMessageMediaSource.fileUpload
                )
            )
        }
        return null
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        val loadMoreConversationMessagesSharedSingle = loadMoreConversationMessagesSharedSingleLock.withLock {
            loadMoreConversationMessagesSharedSingleMap.getOrPut(conversationId) {
                sharedSingleIn(repoScope) {
                    gqlOperationHelper.loadMoreConversationMessagesInCache(conversationId)
                }
            }
        }
        loadMoreConversationMessagesSharedSingle.await()
    }

    override suspend fun sendMessage(message: Message) {
        val messageId = message.message.id
        if (messageId !is MessageId.Local) {
            return
        }
        localMessageDataSource.putMessage(message.modify(SendStatus.Sending))
        runCatchingCancellable {
            when (message) {
                is Message.Deleted -> TODO()
                is Message.Media.Document -> sendMediaMessageImpl(message, messageId)
                is Message.Media.Image -> sendMediaMessageImpl(message, messageId)
                is Message.Text -> sendTextMessageImpl(message, messageId)
            }
        }.onFailure {
            localMessageDataSource.putMessage(
                message.modify(SendStatus.ErrorSending)
            )
        }
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        TODO("Not yet implemented")
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messsageId: MessageId) {
        TODO("Not yet implemented")
    }

    private suspend fun sendMediaMessageImpl(
        mediaMessage: Message.Media<*, *>,
        messageId: MessageId.Local
    ) {
        val mediaSource = mediaMessage.mediaSource
        if (mediaSource !is FileSource.Local) {
            return
        }
        val fileUploadId = fileUploadRepository.uploadFile(mediaSource.fileLocal.uri)
        when (mediaMessage) {
            is Message.Media.Document -> {
                gqlMessageDataSource.sendDocumentMessage(
                    mediaMessage.message.conversationId,
                    messageId.clientId,
                    fileUploadId
                )
            }
            is Message.Media.Image -> {
                gqlMessageDataSource.sendImageMessage(
                    mediaMessage.message.conversationId,
                    messageId.clientId,
                    fileUploadId
                )
            }
        }
    }

    private suspend fun sendTextMessageImpl(message: Message.Text, messageId: MessageId.Local) {
        gqlMessageDataSource.sendTextMessage(
            message.message.conversationId,
            messageId.clientId,
            message.text
        )
    }
}
