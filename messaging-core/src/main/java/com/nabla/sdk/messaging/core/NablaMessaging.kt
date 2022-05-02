package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.data.exception.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.NablaMessaging.Companion.getInstance
import com.nabla.sdk.messaging.core.NablaMessaging.Companion.initialize
import com.nabla.sdk.messaging.core.data.conversation.ConversationRepositoryMock
import com.nabla.sdk.messaging.core.data.message.MessageRepositoryMock
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Main entry-point for SDK messaging features.
 *
 * Mandatory: before any interaction with messaging features make sure you
 * successfully authenticated your user by calling [NablaCore.authenticate].
 *
 * We recommend you reuse the same instance for all interactions,
 * check documentation of [initialize] and [getInstance].
 */
class NablaMessaging private constructor(
    coreContainer: CoreContainer,
) {

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository,
        coreContainer.exceptionMapper,
    )

    private val useMock = false // TODO remove when everything is plugged in

    private val conversationRepository: ConversationRepository by lazy {
        if (useMock) ConversationRepositoryMock() else messagingContainer.conversationRepository
    }
    private val messageRepository: MessageRepository by lazy {
        if (useMock) MessageRepositoryMock() else messagingContainer.messageRepository
    }

    val logger: Logger = coreContainer.logger

    /**
     * Watch the list of conversations the current user is involved in.
     *
     * @see WatchPaginatedResponse for pagination mechanism.
     *
     * Returned flow might throw any of [NablaException] children.
     */
    fun watchConversations(): Flow<WatchPaginatedResponse<List<Conversation>>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                conversationRepository.loadMoreConversations()
            }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
        }

        return conversationRepository.watchConversations()
            .map { paginatedConversations ->
                WatchPaginatedResponse(
                    content = paginatedConversations.items,
                    loadMore = if (paginatedConversations.hasMore) {
                        loadMoreCallback
                    } else {
                        null
                    },
                )
            }
            .catchAndRethrowAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Create a new conversation on behalf of the current user.
     *
     * Reference the returned [Conversation.id] for further actions on that freshly created conversation: send message, mark as read, etc.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun createConversation(): Result<Conversation> {
        return runCatchingCancellable {
            conversationRepository.createConversation()
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Watch the list of messages in a conversation.
     * The current user should be involved in that conversation or a security error will be raised.
     *
     * @see WatchPaginatedResponse for pagination mechanism.
     *
     * Returned flow might throw any of [NablaException] children.
     *
     * @param conversationId the id from [Conversation.id].
     */
    fun watchConversationMessages(conversationId: ConversationId): Flow<WatchPaginatedResponse<ConversationWithMessages>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                messageRepository.loadMoreMessages(conversationId)
            }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
        }

        return messageRepository.watchConversationMessages(conversationId)
            .map { paginatedConversationWithMessages ->
                WatchPaginatedResponse(
                    content = paginatedConversationWithMessages.conversationWithMessages,
                    loadMore = if (paginatedConversationWithMessages.hasMore) {
                        loadMoreCallback
                    } else null
                )
            }
            .catchAndRethrowAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Send a new message in the conversation referenced by its [Message.conversationId].
     *
     * This will immediately append [message] to the list of messages in the conversation
     * while making the necessary network query (optimistic behavior).
     *
     * A successful sending will result in the message's [Message.sendStatus] changing to [SendStatus.Sent]
     * and [Message.id] changing to [MessageId.Remote]. While failures will keep a [MessageId.Local] id
     * and change status to [SendStatus.ErrorSending].
     *
     * @param message message to send, check `Message.**.new(..)` helpers to create new messages.
     *
     * @see Message.Text.new
     * @see Message.Media.Image.new
     * @see Message.Media.Document.new
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun sendMessage(message: Message): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.sendMessage(message)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Retry sending a message for which [Message.sendStatus] is [SendStatus.ErrorSending].
     *
     * @param localMessageId the [Message.id] which is guaranteed to be [MessageId.Local] if status is [SendStatus.ErrorSending].
     * @param conversationId concerned conversation, find it in [Message.conversationId].
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.retrySendingMessage(conversationId, localMessageId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Change the current user typing status in the conversation.
     *
     * IMPORTANT: This is an ephemeral operation, if you want to keep your user marked as actively typing
     *            you should call this with isTyping=true at least once every 20 seconds.
     *
     * Call with isTyping=false to immediately mark the user as not typing anymore.
     * Typical use case is when the user deletes their draft.
     *
     * Please note that a successful call to [sendMessage] is enough to set typing to false,
     * so calling both will simply be a needless redundancy.
     *
     * As this will always result in a network call, please avoid overuse. For instance, you don't want
     * to call this each time the user types a new char, add a debounce instead.
     *
     * @param conversationId id of the conversation where user is/isn't actively typing.
     * @param isTyping whether user is actively typing or not.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.setTyping(conversationId, isTyping)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Acknowledge that the current user has seen all messages in it.
     * Will result in all messages sent before current timestamp to be marked as read.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            conversationRepository.markConversationAsRead(conversationId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    /**
     * Delete a message in the conversation. Current user should be its author.
     *
     * This will change the message type to [Message.Deleted].
     *
     * While this works for both messages that were successfully sent and those that failed sending,
     * calling [deleteMessage] on a message being currently in status [SendStatus.Sending] is very likely
     * to have noop or unexpected behavior.
     *
     * If you want to delete a message being currently in sending status,
     * please cancel the suspendable call to [sendMessage].
     *
     * @param conversationId concerned conversation, find it in [Message.conversationId].
     * @param id id of message to be deleted.
     *
     * @return [Result] of the operation, eventual errors will be of type [NablaException].
     */
    @CheckResult
    suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.deleteMessage(conversationId, id)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    companion object {
        private var defaultSingletonInstance: NablaMessaging? = null

        /**
         * Lazy initializer for the singleton of [NablaMessaging].
         * Relies on the singleton in [NablaCore.getInstance].
         *
         * Use for all interactions with the messaging SDK,
         * unless you prefer maintaining your own instances.
         * @see initialize
         */
        fun getInstance(): NablaMessaging {
            synchronized(this) {
                return defaultSingletonInstance ?: run {
                    val instance = initialize(NablaCore.getInstance())
                    defaultSingletonInstance = instance
                    instance
                }
            }
        }

        /**
         * Creator of a custom instance of [NablaMessaging].
         *
         * Unlike the singleton provided in [getInstance], you have the responsibility
         * of maintaining a reference to the returned instance.
         */
        fun initialize(nablaCore: NablaCore): NablaMessaging = NablaMessaging(nablaCore.coreContainer)
    }
}
