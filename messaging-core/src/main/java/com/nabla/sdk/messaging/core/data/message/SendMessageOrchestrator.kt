package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.core.domain.entity.InternalException
import com.nabla.sdk.messaging.core.data.conversation.GqlConversationDataSource
import com.nabla.sdk.messaging.core.data.conversation.LocalConversationDataSource
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

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
    ): MessageId.Local {
        val messageId = message.id.requireLocal()
        localMessageDataSource.putMessage(conversationId, message.modify(SendStatus.Sending))

        val localConversation = if (conversationId is ConversationId.Local) {
            localConversationDataSource.watch(conversationId).first()
        } else null

        runCatching { // we're interested in cancellations
            when (val creationState = localConversation?.creationState) {
                null -> {
                    sendMessageOp(conversationId.requireRemote(), message)
                }
                is LocalConversation.CreationState.Created -> {
                    sendMessageOp(creationState.remoteId, message)
                }
                is LocalConversation.CreationState.ErrorCreating -> {
                    createConversationWithMessage(localConversation, message)
                }
                LocalConversation.CreationState.Creating -> {
                    awaitConversationCreatedThenSendMessage(localConversation, message)
                }
                LocalConversation.CreationState.ToBeCreated -> {
                    createConversationWithMessage(localConversation, message)
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
        message: Message
    ) {
        runCatching {
            localConversationDataSource.update(
                localConversation.copy(
                    creationState = LocalConversation.CreationState.Creating
                )
            )

            val sendMessageInput = messageMapper.messageToGqlSendMessageInput(message) {
                messageFileUploader.uploadFile(this)
            }
            gqlConversationDataSource.createConversation(
                localConversation.title,
                localConversation.providerIds,
                sendMessageInput,
            ) { remoteConversationUuid ->
                localConversationDataSource.update(
                    localConversation.copy(
                        creationState = LocalConversation.CreationState.Created(
                            ConversationId.Remote(
                                clientId = localConversation.localId.clientId,
                                remoteId = remoteConversationUuid,
                            )
                        ),
                    )
                )
            }
        }.onFailure { throwable ->
            when (throwable) {
                is CancellationException -> {
                    localConversationDataSource.remove(localConversation.localId)
                }
                else -> {
                    localConversationDataSource.update(
                        localConversation.copy(
                            creationState = LocalConversation.CreationState.ErrorCreating(throwable)
                        )
                    )
                }
            }
            throw throwable
        }.onSuccess {
            retryFailedLocalMessages(it)
        }
    }

    private suspend fun retryFailedLocalMessages(conversation: Conversation) {
        localMessageDataSource.watchLocalMessages(conversation.id).first()
            .filter { it.sendStatus == SendStatus.ErrorSending }
            .onEach {
                sendMessage(it, conversation.id)
            }
    }

    private suspend fun awaitConversationCreatedThenSendMessage(
        localConversation: LocalConversation,
        message: Message,
    ) {
        // Only to keep order of messages instead of send all messages when conversation is created.
        fairAwaitConversationCreatedMutex.withLock {
            val remoteConversationId =
                localConversationDataSource.watch(localConversation.localId)
                    .map { it.creationState }
                    .onEach {
                        if (it is LocalConversation.CreationState.ErrorCreating) {
                            throw InternalException(it.cause)
                        }
                    }
                    .filterIsInstance<LocalConversation.CreationState.Created>()
                    .first()
                    .remoteId
            sendMessageOp(remoteConversationId, message)
        }
    }

    private suspend fun sendMessageOp(
        remoteConversationId: ConversationId.Remote,
        message: Message,
    ) {
        gqlConversationContentDataSource.sendMessage(
            remoteConversationId,
            messageMapper.messageToGqlSendMessageInput(message) {
                messageFileUploader.uploadFile(this)
            }
        )
    }
}
