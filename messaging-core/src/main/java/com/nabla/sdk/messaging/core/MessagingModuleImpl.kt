package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.CoroutineHelpers.catchAndRethrowAsNablaException
import com.nabla.sdk.core.data.exception.CoroutineHelpers.mapFailure
import com.nabla.sdk.core.data.exception.CoroutineHelpers.mapFailureAsNablaException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.helper.AuthHelper.authenticatable
import com.nabla.sdk.core.domain.helper.FlowConnectionStateAwareHelper.restartWhenConnectionReconnects
import com.nabla.sdk.core.domain.helper.PaginationHelper.wrapAsResponsePaginatedContent
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.ProviderMissingPermissionException
import com.nabla.sdk.messaging.core.domain.entity.ProviderNotFoundException
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

internal class MessagingModuleImpl internal constructor(
    coreContainer: CoreContainer,
) : MessagingModule, NablaMessagingClient {

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.coreGqlMapper,
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository,
        coreContainer.exceptionMapper,
        coreContainer.sessionClient,
        coreContainer.clock,
        coreContainer.uuidGenerator,
        coreContainer.videoCallModule,
        coreContainer.eventsConnectionState,
        coreContainer.backgroundScope,
    )

    private val conversationRepository: ConversationRepository by lazy {
        messagingContainer.conversationRepository
    }
    private val conversationContentRepository: ConversationContentRepository by lazy {
        messagingContainer.conversationContentRepository
    }

    override val logger: Logger = coreContainer.logger

    override fun watchConversations(): Flow<Response<PaginatedContent<List<Conversation>>>> {
        return conversationRepository.watchConversations()
            .wrapAsResponsePaginatedContent(
                conversationRepository::loadMoreConversations,
                messagingContainer.nablaExceptionMapper,
            ).authenticatable(messagingContainer.sessionClient)
            .restartWhenConnectionReconnects(messagingContainer.eventsConnectionStateFlow)
    }

    @CheckResult
    override suspend fun createConversationWithMessage(
        message: MessageInput,
        title: String?,
        providerIds: List<Uuid>?
    ): Result<Conversation> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationRepository.createConversation(message, title, providerIds)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
            .mapFailure { error ->
                if (error is ServerException) {
                    when (error.code) {
                        ProviderNotFoundException.ERROR_CODE -> ProviderNotFoundException(cause = error)
                        ProviderMissingPermissionException.ERROR_CODE -> ProviderMissingPermissionException(cause = error)
                        else -> error
                    }
                } else error
            }
    }

    override fun startConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        return conversationRepository.createLocalConversation(title, providerIds)
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Response<Conversation>> {
        return conversationRepository.watchConversation(conversationId)
            .restartWhenConnectionReconnects(messagingContainer.eventsConnectionStateFlow)
            .authenticatable(messagingContainer.sessionClient)
            .catchAndRethrowAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    override fun watchConversationItems(conversationId: ConversationId): Flow<Response<PaginatedContent<List<ConversationItem>>>> {
        return conversationContentRepository.watchConversationItems(conversationId)
            .wrapAsResponsePaginatedContent(
                { conversationContentRepository.loadMoreMessages(conversationId) },
                messagingContainer.nablaExceptionMapper,
            )
            .authenticatable(messagingContainer.sessionClient)
            .restartWhenConnectionReconnects(messagingContainer.eventsConnectionStateFlow)
    }

    override suspend fun sendMessage(
        input: MessageInput,
        conversationId: ConversationId,
        replyTo: MessageId.Remote?,
    ): Result<MessageId.Local> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationContentRepository.sendMessage(input, conversationId, replyTo)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationContentRepository.retrySendingMessage(conversationId, localMessageId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationContentRepository.setTyping(conversationId, isTyping)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationRepository.markConversationAsRead(conversationId)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @CheckResult
    override suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit> {
        return runCatchingCancellable {
            messagingContainer.sessionClient.authenticatableOrThrow()
            conversationContentRepository.deleteMessage(conversationId, id)
        }.mapFailureAsNablaException(messagingContainer.nablaExceptionMapper)
    }

    @NablaInternal
    override val internalClient: Unit
        get() = Unit
}
