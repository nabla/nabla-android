package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.nabla.sdk.core.data.exception.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class NablaMessagingClientImpl internal constructor(
    coreContainer: CoreContainer,
) : NablaMessagingClient {

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository,
        coreContainer.exceptionMapper,
        coreContainer.sessionClient,
    )

    private val conversationRepository: ConversationRepository by lazy {
        messagingContainer.conversationRepository
    }
    private val conversationContentRepository: ConversationContentRepository by lazy {
        messagingContainer.conversationContentRepository
    }

    override val logger: Logger = coreContainer.logger

    override fun watchConversations(): Flow<WatchPaginatedResponse<List<Conversation>>> {
        ensureAuthenticatedOrThrow()

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
    override suspend fun createConversation(): Result<Conversation> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationRepository.createConversation()
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Conversation> {
        ensureAuthenticatedOrThrow()

        return conversationRepository.watchConversation(conversationId)
            .catchAndRethrowAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    override fun watchConversationItems(conversationId: ConversationId): Flow<WatchPaginatedResponse<ConversationItems>> {
        ensureAuthenticatedOrThrow()

        val loadMoreCallback = suspend {
            runCatchingCancellable {
                conversationContentRepository.loadMoreMessages(conversationId)
            }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
        }

        return conversationContentRepository.watchConversationItems(conversationId)
            .map { paginatedConversationMessages ->
                WatchPaginatedResponse(
                    content = paginatedConversationMessages.conversationItems,
                    loadMore = if (paginatedConversationMessages.hasMore) {
                        loadMoreCallback
                    } else null
                )
            }
            .catchAndRethrowAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    override suspend fun sendMessage(
        input: MessageInput,
        conversationId: ConversationId
    ): Result<MessageId.Local> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationContentRepository.sendMessage(input, conversationId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationContentRepository.retrySendingMessage(conversationId, localMessageId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationContentRepository.setTyping(conversationId, isTyping)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationRepository.markConversationAsRead(conversationId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit> {
        ensureAuthenticatedOrThrow()

        return runCatchingCancellable {
            conversationContentRepository.deleteMessage(conversationId, id)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    private fun ensureAuthenticatedOrThrow() {
        if (!messagingContainer.sessionClient.isSessionInitialized()) {
            throw NablaException.Authentication.NotAuthenticated
        }
    }
}
