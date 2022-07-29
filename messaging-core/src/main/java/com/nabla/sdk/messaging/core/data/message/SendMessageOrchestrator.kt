package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.messaging.core.data.conversation.GqlConversationDataSource
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SendMessageOrchestrator constructor(
    private val localConversationDataSource: LocalConversationDataSource,
    private val localMessageDataSource: LocalMessageDataSource,
    private val gqlConversationDataSource: GqlConversationDataSource,
    private val gqlConversationContentDataSource: GqlConversationContentDataSource,
    private val messageMapper: MessageMapper,
    private val messageFileUploader: MessageFileUploader,
) {

    private val fairAwaitConversationCreatedMutex: Mutex = Mutex()

    suspend fun sendMessage(
        message: Message,
        conversationId: ConversationId,
        replyTo: MessageId.Remote?,
        isRetried: Boolean = false,
    ): MessageId.Local {
        val messageId = message.id.requireLocal()
        localMessageDataSource.putMessage(conversationId, message.modify(SendStatus.Sending))

        val localConversation = if (conversationId is ConversationId.Local) {
            localConversationDataSource.watch(conversationId).first()
        } else null

        runCatching { // we're interested in cancellations
            when (val creationState = localConversation?.creationState) {
                null -> {
                    sendMessageOp(conversationId.requireRemote(), message, replyTo)
                }
                is LocalConversation.CreationState.Created -> {
                    sendMessageOp(creationState.remoteId, message, replyTo)
                }
                LocalConversation.CreationState.ErrorCreating -> {
                    if (isRetried) {
                        createConversationWithMessage(localConversation, message, replyTo)
                    } else {
                        awaitConversationCreatedThenSendMessage(localConversation, message, replyTo)
                    }
                }
                LocalConversation.CreationState.Creating -> {
                    awaitConversationCreatedThenSendMessage(localConversation, message, replyTo)
                }
                LocalConversation.CreationState.ToBeCreated -> {
                    createConversationWithMessage(localConversation, message, replyTo)
                }
            }
        }.onFailure { throwable ->
            when (throwable) {
                is CancellationException -> {
                    localMessageDataSource.removeMessage(conversationId, messageId)
                }
                else -> {
                    localMessageDataSource.putMessage(conversationId, message.modify(SendStatus.ErrorSending))
                }
            }
            throw throwable
        }.onSuccess {
            val sentMessage = message.modify(SendStatus.Sent)
            localMessageDataSource.putMessage(conversationId, sentMessage)
        }

        return messageId
    }

    private suspend fun createConversationWithMessage(
        localConversation: LocalConversation,
        message: Message,
        replyTo: MessageId.Remote?
    ) {
        runCatching {
            localConversationDataSource.update(
                localConversation.copy(
                    creationState = LocalConversation.CreationState.Creating
                )
            )
            val sendMessageInput = messageMapper.messageToGqlSendMessageInput(message, replyTo) {
                messageFileUploader.uploadFile(this)
            }
            val conversation = gqlConversationDataSource.createConversation(
                localConversation.title,
                localConversation.providerIds,
                sendMessageInput
            )
            localConversationDataSource.update(
                localConversation.copy(
                    creationState = LocalConversation.CreationState.Created(
                        conversation.id.requireRemote()
                    ),
                )
            )
        }.onFailure { throwable ->
            when (throwable) {
                is CancellationException -> {
                    localConversationDataSource.remove(localConversation.localId)
                }
                else -> {
                    localConversationDataSource.update(
                        localConversation.copy(
                            creationState = LocalConversation.CreationState.ErrorCreating
                        )
                    )
                }
            }
            throw throwable
        }
    }

    private suspend fun awaitConversationCreatedThenSendMessage(
        localConversation: LocalConversation,
        message: Message,
        replyTo: MessageId.Remote?
    ) {
        // Only to keep order of messages instead of send all messages when conversation is created.
        fairAwaitConversationCreatedMutex.withLock {
            val remoteConversationId =
                localConversationDataSource.waitConversationCreated(localConversation.localId)
            sendMessageOp(remoteConversationId, message, replyTo)
        }
    }

    private suspend fun sendMessageOp(
        remoteConversationId: ConversationId.Remote,
        message: Message,
        replyTo: MessageId.Remote?
    ) {
        gqlConversationContentDataSource.sendMessage(
            remoteConversationId,
            messageMapper.messageToGqlSendMessageInput(message, replyTo) {
                messageFileUploader.uploadFile(this)
            }
        )
    }
}
