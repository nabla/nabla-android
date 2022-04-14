package com.nabla.sdk.messaging.core

import androidx.annotation.CheckResult
import com.nabla.sdk.core.NablaCore
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

class NablaMessaging private constructor(coreContainer: CoreContainer) {
    constructor(core: NablaCore) : this(core.coreContainer)

    private val messagingContainer = MessagingContainer(
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository
    )

    private val conversationRepository: ConversationRepository by lazy { messagingContainer.conversationRepository }
    private val mockConversationRepository = ConversationRepositoryMock()
    private val messageRepository: MessageRepository by lazy { messagingContainer.messageRepository }
    private val mockMessageRepository = MessageRepositoryMock()

    fun watchConversations(): Flow<WatchPaginatedResponse<List<Conversation>>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                mockConversationRepository.loadMoreConversations()
            }
        }

        return mockConversationRepository.watchConversations()
            .map { paginatedConversations ->
                WatchPaginatedResponse(
                    items = paginatedConversations.items,
                    loadMore = if (paginatedConversations.hasMore) { loadMoreCallback } else { null },
                )
            }
    }

    @CheckResult
    suspend fun createConversation(): Result<Conversation> {
        return runCatchingCancellable {
            mockConversationRepository.createConversation()
        }
    }

    fun watchConversationMessages(conversationId: ConversationId): Flow<WatchPaginatedResponse<ConversationWithMessages>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                mockMessageRepository.loadMoreMessages(conversationId)
            }
        }

        return mockMessageRepository.watchConversationMessages(conversationId)
            .map { paginatedConversationWithMessages ->
                WatchPaginatedResponse(
                    items = paginatedConversationWithMessages.conversationWithMessages,
                    loadMore = if (paginatedConversationWithMessages.hasMore) { loadMoreCallback } else null
                )
            }
    }

    @CheckResult
    suspend fun sendMessage(message: Message): Result<Unit> {
        return runCatchingCancellable {
            mockMessageRepository.sendMessage(message)
        }
    }

    @CheckResult
    suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            mockMessageRepository.retrySendingMessage(conversationId, localMessageId)
        }
    }

    @CheckResult
    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        mockMessageRepository.setTyping(conversationId, isTyping)
    }

    @CheckResult
    suspend fun markConversationAsRead(conversationId: ConversationId) {
        mockConversationRepository.markConversationAsRead(conversationId)
    }

    @CheckResult
    suspend fun deleteMessage(conversationId: ConversationId, id: MessageId) {
        mockMessageRepository.deleteMessage(conversationId, id)
    }

    companion object {
        val instance = NablaMessaging(NablaCore.instance.coreContainer)
    }
}
