package com.nabla.sdk.messaging.core

import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.data.conversation.ConversationRepositoryMock
import com.nabla.sdk.messaging.core.data.message.MessageRepositoryMock
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

class NablaMessaging private constructor(coreContainer: CoreContainer) {
    constructor(core: NablaCore) : this(core.coreContainer)

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository
    )

    private val conversationRepository: ConversationRepository by lazy { messagingContainer.conversationRepository }
    private val mockConversationRepository = ConversationRepositoryMock()
    private val messageRepository: MessageRepository by lazy { messagingContainer.messageRepository }
    private val mockMessageRepository = MessageRepositoryMock()

    fun watchConversations(): Flow<PaginatedList<Conversation>> {
        return mockConversationRepository.watchConversations()
    }

    suspend fun createConversation() {
        mockConversationRepository.createConversation()
    }

    suspend fun loadMoreConversations() {
        mockConversationRepository.loadMoreConversations()
    }

    fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
        return mockMessageRepository.watchConversationMessages(conversationId)
    }

    suspend fun sendMessage(message: Message) {
        mockMessageRepository.sendMessage(message)
    }

    suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        mockMessageRepository.setTyping(conversationId, isTyping)
    }

    suspend fun loadMoreMessages(conversationId: ConversationId) {
        mockMessageRepository.loadMoreMessages(conversationId)
    }

    suspend fun retrySendingMessage(conversationId: ConversationId, local: MessageId.Local) {
        mockMessageRepository.retrySendingMessage(conversationId, local)
    }

    suspend fun markConversationAsRead(conversationId: ConversationId) {
        mockConversationRepository.markConversationAsRead(conversationId)
    }

    suspend fun deleteMessage(conversationId: ConversationId, id: MessageId) {
        mockMessageRepository.deleteMessage(conversationId, id)
    }

    companion object {
        val instance = NablaMessaging(NablaCore.instance.coreContainer)
    }
}
