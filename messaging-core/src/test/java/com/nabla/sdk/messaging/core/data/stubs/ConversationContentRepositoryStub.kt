package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.messaging.core.data.message.PaginatedConversationItems
import com.nabla.sdk.messaging.core.domain.boundary.ConversationContentRepository
import com.nabla.sdk.messaging.core.domain.entity.BaseMessage
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class ConversationContentRepositoryStub(private val idlingRes: CountingIdlingResource) : ConversationContentRepository {
    private val provider = User.Provider.fake()
    private val flowMutex = Mutex()
    private val messagesListFlow = MutableStateFlow(
        buildList {
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
        }
    )

    override fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems> {
        return messagesListFlow.map { messages ->
            PaginatedConversationItems(
                conversationItems = ConversationItems.fake(
                    conversation = Conversation.fake(),
                    messages = messages.sortedByDescending { it.baseMessage.createdAt }
                ),
                hasMore = true,
            )
        }
            .onStart {
                delayWithIdlingRes(idlingRes, 1.seconds)
                if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
            }
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        println("load more messages")
        delayWithIdlingRes(idlingRes, 1.seconds)
        if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
        flowMutex.withLock {
            val oldestInstant = messagesListFlow.value.minOf { it.baseMessage.createdAt }
            messagesListFlow.value =
                messagesListFlow.value + (1..10).map { Message.randomFake(sentAt = oldestInstant.minus(12.hours).minus(it.minutes)) }
        }
    }

    override suspend fun sendMessage(
        input: MessageInput,
        conversationId: ConversationId
    ): MessageId.Local {
        val messageId = MessageId.Local(Uuid.randomUUID())
        val baseMessage = BaseMessage(messageId, Clock.System.now(), MessageSender.Patient, SendStatus.ToBeSent, conversationId)
        val message = when (input) {
            is MessageInput.Media.Document -> Message.Media.Document(baseMessage, input.mediaSource)
            is MessageInput.Media.Image -> Message.Media.Image(baseMessage, input.mediaSource)
            is MessageInput.Text -> Message.Text(baseMessage, input.text)
        }

        flowMutex.withLock { messagesListFlow.value = messagesListFlow.value + listOf(message) }
        delayWithIdlingRes(idlingRes, 1.seconds)
        return flowMutex.withLock {
            val newMessage = message.modify(if ((message as? Message.Text)?.text?.contains("fail") == true) SendStatus.ErrorSending else SendStatus.Sent)
            messagesListFlow.value = messagesListFlow.value - setOf(message) + listOf(
                newMessage
            )
            messageId
        }
    }

    private fun Message.Companion.randomFake(sentAt: Instant): Message {
        val sender = if (Random.nextBoolean()) MessageSender.Patient else MessageSender.Provider(User.Provider.fake())
        return when (Random.nextInt() % 100) {
            in 0..70 -> Message.Text.fake(sender = sender, sentAt = sentAt)
            in 71..85 -> Message.Media.Image.fake(sender = sender, sentAt = sentAt)
            else -> Message.Media.Document.fake(sender = sender, sentAt = sentAt)
        }
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        flowMutex.withLock {
            messagesListFlow.value = messagesListFlow.value.map {
                if (it.baseMessage.id == localMessageId && it.baseMessage.sendStatus == SendStatus.ErrorSending) {
                    it.modify(SendStatus.Sending)
                } else it
            }
        }
        delayWithIdlingRes(idlingRes, 1.seconds)
        flowMutex.withLock {
            messagesListFlow.value = messagesListFlow.value.map {
                if (it.baseMessage.id == localMessageId && it.baseMessage.sendStatus == SendStatus.Sending) {
                    it.modify(SendStatus.Sent)
                } else it
            }
        }
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        // TODO
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId) {
        flowMutex.withLock {
            messagesListFlow.value = messagesListFlow.value.map {
                if (it.baseMessage.id == messageId) {
                    Message.Deleted(it.baseMessage)
                } else it
            }
        }
    }

    companion object {
        private const val MOCK_ERRORS = false
    }
}
