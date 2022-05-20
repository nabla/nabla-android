package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.WatchPaginatedResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NablaMessagingClientStub(
    val idlingRes: CountingIdlingResource = CountingIdlingResource("Stubs Idling Res", true),
) : NablaMessagingClient {
    var isTyping = false

    internal val conversationRepository = ConversationRepositoryStub(idlingRes)
    internal val messageRepository = ConversationContentRepositoryStub(idlingRes, conversationRepository)

    override val logger: Logger = LoggerImpl

    override fun watchConversations(): Flow<WatchPaginatedResponse<List<Conversation>>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                conversationRepository.loadMoreConversations()
            }
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
    }

    override suspend fun createConversation(): Result<Conversation> {
        return runCatchingCancellable {
            conversationRepository.createConversation()
        }
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Conversation> {
        return conversationRepository.watchConversation(conversationId)
    }

    override fun watchConversationItems(conversationId: ConversationId): Flow<WatchPaginatedResponse<ConversationItems>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                messageRepository.loadMoreMessages(conversationId)
            }
        }

        return messageRepository.watchConversationItems(conversationId)
            .map { paginatedConversationItems ->
                WatchPaginatedResponse(
                    content = paginatedConversationItems.conversationItems,
                    loadMore = if (paginatedConversationItems.hasMore) {
                        loadMoreCallback
                    } else null
                )
            }
    }

    override suspend fun sendMessage(input: MessageInput, conversationId: ConversationId): Result<MessageId.Local> {
        isTyping = false
        return runCatchingCancellable {
            messageRepository.sendMessage(input, conversationId)
        }
    }

    override suspend fun retrySendingMessage(localMessageId: MessageId.Local, conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.retrySendingMessage(conversationId, localMessageId)
        }
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean): Result<Unit> {
        this.isTyping = isTyping

        return Result.success(Unit)
    }

    override suspend fun markConversationAsRead(conversationId: ConversationId): Result<Unit> {
        return runCatchingCancellable {
            conversationRepository.markConversationAsRead(conversationId)
        }
    }

    override suspend fun deleteMessage(conversationId: ConversationId, id: MessageId): Result<Unit> {
        return runCatchingCancellable {
            messageRepository.deleteMessage(conversationId, id)
        }
    }
}
