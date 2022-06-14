package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.core.domain.boundary.FileUploadRepository
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.kotlin.SharedSingle
import com.nabla.sdk.core.kotlin.sharedSingleIn
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

internal class ConversationContentRepositoryImpl(
    private val repoScope: CoroutineScope,
    private val localMessageDataSource: LocalMessageDataSource,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
    private val fileUploadRepository: FileUploadRepository,
    private val clock: Clock,
    private val uuidGenerator: UuidGenerator,
    private val logger: Logger,
) : ConversationContentRepository {

    private val loadMoreConversationMessagesSharedSingleLock = Mutex()
    private val loadMoreConversationMessagesSharedSingleMap = mutableMapOf<ConversationId, SharedSingle<Unit>>()

    override fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems> {
        val gqlConversationAndMessagesFlow = gqlConversationContentDataSource.watchConversationItems(conversationId)
        val localMessagesFlow = localMessageDataSource.watchLocalMessages(conversationId)
        val messageFlow = gqlConversationAndMessagesFlow.combine(localMessagesFlow) { gqlConversationAndMessages, localMessages ->
            return@combine combineGqlAndLocalInfo(localMessages, gqlConversationAndMessages)
        }
        return messageFlow
    }

    private fun combineGqlAndLocalInfo(
        localMessages: Collection<Message>,
        gqlPaginatedConversationAndMessages: PaginatedConversationItems,
    ): PaginatedConversationItems {
        val (gqlConversationItemMessages, gqlConversationItemNotMessages) = gqlPaginatedConversationAndMessages.conversationItems.items.partition { conversationItem ->
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
        return gqlPaginatedConversationAndMessages.copy(
            conversationItems = gqlPaginatedConversationAndMessages.conversationItems.copy(
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
        val baseMessage = BaseMessage(
            MessageId.Local(uuidGenerator.generate()),
            clock.now(),
            MessageAuthor.Patient,
            SendStatus.Sending,
            conversationId,
            replyTo = replyTo?.let {
                gqlConversationContentDataSource.findMessageInConversationCache(conversationId, it).also { message ->
                    if (message == null) logger.warn("Reply to message not found in cache: $it")
                }
            },
        )
        val message = when (input) {
            is MessageInput.Media.Document -> Message.Media.Document(baseMessage, input.mediaSource)
            is MessageInput.Media.Image -> Message.Media.Image(baseMessage, input.mediaSource)
            is MessageInput.Text -> Message.Text(baseMessage, input.text)
            is MessageInput.Media.Audio -> Message.Media.Audio(baseMessage, input.mediaSource)
        }

        return sendMessage(
            message,
            replyTo = replyTo, // prefer the id param over [message.replyTo] as we might have not found it in the cache
        )
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        val localMessage = localMessageDataSource.getMessage(conversationId, localMessageId)
        if (localMessage != null) {
            sendMessage(localMessage)
        } else {
            throw NablaException.MessageNotFound(conversationId, localMessageId)
        }
    }

    private suspend fun sendMessage(message: Message, replyTo: MessageId? = message.replyTo?.id): MessageId.Local {
        val messageId = message.id as? MessageId.Local
            ?: throw NablaException.InvalidMessage("Can't send a message that is not a local one")

        localMessageDataSource.putMessage(message.modify(SendStatus.Sending))

        runCatching { // we're interested in cancellations
            when (message) {
                is Message.Deleted -> throw NablaException.InvalidMessage("Can't send a deleted message")
                is Message.Media<*, *> -> sendMediaMessageOp(message, messageId)
                is Message.Text -> sendTextMessageOp(message, messageId, replyTo = replyTo)
            }
        }.onFailure { throwable ->
            when (throwable) {
                is CancellationException ->
                    localMessageDataSource
                        .removeMessage(message.conversationId, messageId)
                else ->
                    localMessageDataSource
                        .putMessage(message.modify(SendStatus.ErrorSending))
            }

            throw throwable
        }.onSuccess {
            val sentMessage = message.modify(SendStatus.Sent)
            localMessageDataSource.putMessage(sentMessage)
        }

        return messageId
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        gqlConversationContentDataSource.setTyping(conversationId, isTyping)
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId) {
        when (messageId) {
            is MessageId.Local -> localMessageDataSource.removeMessage(conversationId, messageId)
            is MessageId.Remote -> gqlConversationContentDataSource.deleteMessage(conversationId, messageId)
        }
    }

    private suspend fun sendMediaMessageOp(
        mediaMessage: Message.Media<*, *>,
        messageId: MessageId.Local,
    ) {
        val mediaSource = mediaMessage.mediaSource
        if (mediaSource !is FileSource.Local) {
            throw NablaException.InvalidMessage("Can't send a media message with a media source that is not local")
        }

        val fileUploadId = fileUploadRepository.uploadFile(mediaSource.fileLocal.uri, mediaMessage.fileName, mediaMessage.mimeType)

        when (mediaMessage) {
            is Message.Media.Document -> {
                gqlConversationContentDataSource.sendDocumentMessage(
                    conversationId = mediaMessage.baseMessage.conversationId,
                    clientId = messageId.clientId,
                    fileUploadId = fileUploadId,
                    replyToMessageId = mediaMessage.baseMessage.replyTo?.id?.remoteId,
                )
            }
            is Message.Media.Image -> {
                gqlConversationContentDataSource.sendImageMessage(
                    mediaMessage.baseMessage.conversationId,
                    messageId.clientId,
                    fileUploadId,
                    replyToMessageId = mediaMessage.baseMessage.replyTo?.id?.remoteId,
                )
            }
            is Message.Media.Audio -> {
                gqlConversationContentDataSource.sendAudioMessage(
                    mediaMessage.baseMessage.conversationId,
                    messageId.clientId,
                    fileUploadId,
                    replyToMessageId = mediaMessage.baseMessage.replyTo?.id?.remoteId,
                )
            }
        }
    }

    private suspend fun sendTextMessageOp(message: Message.Text, messageId: MessageId.Local, replyTo: MessageId?) {
        gqlConversationContentDataSource.sendTextMessage(
            conversationId = message.baseMessage.conversationId,
            clientId = messageId.clientId,
            text = message.text,
            replyToMessageId = replyTo?.remoteId,
        )
    }
}
