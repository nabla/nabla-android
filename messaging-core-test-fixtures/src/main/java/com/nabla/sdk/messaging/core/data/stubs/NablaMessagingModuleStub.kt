package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.core.domain.entity.LogcatLogger
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NablaMessagingModuleStub(
    val idlingRes: CountingIdlingResource = CountingIdlingResource("Stubs Idling Res", true),
) : MessagingModule, NablaMessagingClient {
    var isTyping = false

    private val conversationRepository = ConversationRepositoryStub(idlingRes)
    private val messageRepository = ConversationContentRepositoryStub(idlingRes, conversationRepository)

    override val logger: Logger = LogcatLogger(logLevel = LogcatLogger.LogLevel.DEBUG)

    override fun watchConversations(): Flow<Response<PaginatedContent<List<Conversation>>>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                conversationRepository.loadMoreConversations()
            }
        }

        return conversationRepository.watchConversations()
            .map { paginatedConversationsResponse ->
                Response(
                    isDataFresh = paginatedConversationsResponse.isDataFresh,
                    refreshingState = paginatedConversationsResponse.refreshingState,
                    data = PaginatedContent(
                        content = paginatedConversationsResponse.data.items,
                        loadMore = if (paginatedConversationsResponse.data.hasMore) {
                            loadMoreCallback
                        } else {
                            null
                        },
                    ),
                )
            }
    }

    override suspend fun createConversationWithMessage(
        message: MessageInput,
        title: String?,
        providerIds: List<Uuid>?,
    ): Result<Conversation> {
        return runCatchingCancellable {
            conversationRepository.createConversation(message, title, providerIds)
        }
    }

    override fun startConversation(title: String?, providerIds: List<Uuid>?): ConversationId.Local {
        return conversationRepository.createLocalConversation(title, providerIds)
    }

    override fun watchConversation(conversationId: ConversationId): Flow<Response<Conversation>> {
        return conversationRepository.watchConversation(conversationId)
    }

    override fun watchConversationItems(conversationId: ConversationId): Flow<Response<PaginatedContent<List<ConversationItem>>>> {
        val loadMoreCallback = suspend {
            runCatchingCancellable {
                messageRepository.loadMoreMessages(conversationId)
            }
        }

        return messageRepository.watchConversationItems(conversationId)
            .map { paginatedConversationItemsResponse ->
                Response(
                    isDataFresh = paginatedConversationItemsResponse.isDataFresh,
                    refreshingState = paginatedConversationItemsResponse.refreshingState,
                    data = PaginatedContent(
                        content = paginatedConversationItemsResponse.data.items,
                        loadMore = if (paginatedConversationItemsResponse.data.hasMore) {
                            loadMoreCallback
                        } else {
                            null
                        },
                    ),
                )
            }
    }

    override suspend fun sendMessage(input: MessageInput, conversationId: ConversationId, replyTo: MessageId.Remote?): Result<MessageId.Local> {
        isTyping = false
        return runCatchingCancellable {
            messageRepository.sendMessage(input, conversationId, replyTo)
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

    @NablaInternal
    override val internalClient: Unit
        get() = Unit
}
