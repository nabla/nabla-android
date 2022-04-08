package com.nabla.sdk.messaging.core.data

import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.fake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class MessageRepositoryMock : MessageRepository {
    override fun watchConversationMessages(conversationId: ConversationId): Flow<ConversationWithMessages> {
        val provider = User.Provider.fake()
        return flowOf(
            ConversationWithMessages.fake(
                conversation = Conversation.fake(providersInConversation = listOf(ProviderInConversation.fake(provider))),
                messages = PaginatedList(
                    hasMore = true,
                    items = buildList {
                        add(
                            Message.Text.fake(
                                sentAt = Clock.System.now().minus(2.days),
                                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                            )
                        )
                        addAll((9 downTo 4).map { Message.Text.fake(sentAt = Clock.System.now().minus(it.minutes)) })
                        add(
                            Message.Text.fake(
                                sender = MessageSender.Provider(provider),
                                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor",
                                sentAt = Clock.System.now().minus(3.minutes),
                            )
                        )

                        add(Message.Media.Image.fake(sentAt = Clock.System.now().minus(2.minutes)))
                        add(Message.Media.Document.fake(sender = MessageSender.Provider(provider), sentAt = Clock.System.now().minus(1.minutes)))
                    }.asReversed()
                )
            )
        )
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        println("load more messages")
    }

    override suspend fun sendMessage(message: Message) {
        // TODO
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        // TODO
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        // TODO
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messsageId: MessageId) {
        // TODO
    }
}
