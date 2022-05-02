package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.data.exception.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.data.conversation.ConversationRepositoryMock
import com.nabla.sdk.messaging.core.data.message.MessageRepositoryMock
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NablaMessaging private constructor(
    coreContainer: CoreContainer
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

    @CheckResult
    suspend fun createConversation(): Result<Conversation> {
        return runCatchingCancellable {
            conversationRepository.createConversation()
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

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

    @CheckResult
    suspend fun sendMessage(message: Message): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.sendMessage(message)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.retrySendingMessage(conversationId, localMessageId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.setTyping(conversationId, isTyping)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            conversationRepository.markConversationAsRead(conversationId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.deleteMessage(conversationId, id)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    companion object {
        private var defaultSingletonInstance: NablaMessaging? = null

        fun getInstance(): NablaMessaging {
            synchronized(this) {
                return defaultSingletonInstance ?: run {
                    val instance = initialize(NablaCore.getInstance())
                    defaultSingletonInstance = instance
                    instance
                }
            }
        }

        fun initialize(nablaCore: NablaCore): NablaMessaging = NablaMessaging(nablaCore.coreContainer)
    }
}
