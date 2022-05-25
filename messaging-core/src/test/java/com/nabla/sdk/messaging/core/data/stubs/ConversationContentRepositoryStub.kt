package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.PaginatedList
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
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal class ConversationContentRepositoryStub(
    private val idlingRes: CountingIdlingResource,
    private val conversationRepositoryStub: ConversationRepositoryStub,
) : ConversationContentRepository {
    private val scope = CoroutineScope(Job())

    private val provider = User.Provider.fake()
    private val flowMutex = Mutex()
    private val messagesFlowPerConversation = mutableMapOf<ConversationId, MutableStateFlow<List<Message>>>()

    override fun watchConversationItems(conversationId: ConversationId): Flow<PaginatedConversationItems> {
        val isNewlyCreated = conversationId in conversationRepositoryStub.newlyCreatedConversationIds
        return messagesFlowPerConversation.getOrPut(conversationId) {
            conversationItemsStateFlow(prepopulate = !isNewlyCreated)
        }
            .map { messages ->
                PaginatedConversationItems(
                    conversationItems = ConversationItems.fake(
                        conversation = conversationRepositoryStub.conversationsFlow.value.items.first { it.id == conversationId },
                        messages = messages.sortedByDescending { it.baseMessage.createdAt }
                    ),
                    hasMore = !isNewlyCreated,
                )
            }
            .onStart {
                delayWithIdlingRes(idlingRes, 100.milliseconds)
                if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
            }
    }

    override suspend fun loadMoreMessages(conversationId: ConversationId) {
        println("load more messages")
        delayWithIdlingRes(idlingRes, 100.milliseconds)
        if (MOCK_ERRORS) if (Random.nextBoolean()) throw ApolloNetworkException()
        flowMutex.withLock {
            val messages = messagesOf(conversationId)
            val oldestInstant = messages.minOf { it.baseMessage.createdAt }
            messagesFlowPerConversation[conversationId]!!.value =
                messages + (1..10).map { Message.randomFake(sentAt = oldestInstant.minus(12.hours).minus(it.minutes)) }
        }
    }

    private fun Message.Companion.randomFake(sentAt: Instant): Message {
        val sender = if (Random.nextBoolean()) MessageSender.Patient else MessageSender.Provider(User.Provider.fake())
        return when (Random.nextInt() % 100) {
            in 0..70 -> Message.Text.fake(sender = sender, sentAt = sentAt)
            in 71..80 -> Message.Media.Image.fake(sender = sender, sentAt = sentAt)
            in 81..90 -> Message.Media.Audio.fake(sender = sender, sentAt = sentAt)
            else -> Message.Media.Document.fake(sender = sender, sentAt = sentAt)
        }
    }

    override suspend fun sendMessage(input: MessageInput, conversationId: ConversationId): MessageId.Local {
        val localId = MessageId.Local(uuid4())
        val baseMessage = BaseMessage(localId, Clock.System.now(), MessageSender.Patient, SendStatus.Sending, conversationId)
        val message = when (input) {
            is MessageInput.Media.Document -> Message.Media.Document(baseMessage, input.mediaSource)
            is MessageInput.Media.Image -> Message.Media.Image(baseMessage, input.mediaSource)
            is MessageInput.Text -> Message.Text(baseMessage, input.text)
        }

        val conversationFlow = messagesFlowPerConversation[conversationId]!!
        flowMutex.withLock { conversationFlow.value = messagesOf(conversationId) + listOf(message) }
        delayWithIdlingRes(idlingRes, 100.milliseconds)
        flowMutex.withLock {
            val newStatus = if ((message as? Message.Text)?.text?.contains("fail") == true) SendStatus.ErrorSending else SendStatus.Sent
            conversationFlow.value = messagesOf(conversationId) - setOf(message) + listOf(message.modify(newStatus))
        }.also {
            if ((message as? Message.Text)?.text?.contains("reply") == true) {
                fakeProviderReplying(conversationId, conversationFlow)
            }
        }
        return localId
    }

    private fun fakeProviderReplying(
        conversationId: ConversationId,
        conversationFlow: MutableStateFlow<List<Message>>,
    ) {
        scope.launch {
            val conversationsFlow = conversationRepositoryStub.conversationsFlow
            val conversation = conversationsFlow.value.items.first { it.id == conversationId }
            val provider = ProviderInConversation.fake(typingAt = Clock.System.now())
            setProviderTyping(conversationsFlow, conversation, provider)

            delayWithIdlingRes(idlingRes, 200.milliseconds)
            conversationFlow.value = messagesOf(conversationId) + Message.Text.fake(
                sender = MessageSender.Provider(provider.provider),
                text = "Here I am!",
            )
        }
    }

    private fun setProviderTyping(
        conversationsFlow: MutableStateFlow<PaginatedList<Conversation>>,
        conversation: Conversation,
        provider: ProviderInConversation,
    ) {
        conversationsFlow.value = conversationsFlow.value.copy(
            conversationsFlow.value.items.map {
                if (it.id == conversation.id) {
                    it.copy(providersInConversation = listOf(provider))
                } else it
            }
        )
    }

    override suspend fun retrySendingMessage(conversationId: ConversationId, localMessageId: MessageId.Local) {
        flowMutex.withLock {
            messagesFlowPerConversation[conversationId]!!.value = messagesOf(conversationId).map {
                if (it.baseMessage.id == localMessageId && it.baseMessage.sendStatus == SendStatus.ErrorSending) {
                    it.modify(SendStatus.Sending)
                } else it
            }
        }
        delayWithIdlingRes(idlingRes, 100.milliseconds)
        flowMutex.withLock {
            messagesFlowPerConversation[conversationId]!!.value = messagesOf(conversationId).map {
                if (it.baseMessage.id == localMessageId && it.baseMessage.sendStatus == SendStatus.Sending) {
                    it.modify(SendStatus.Sent)
                } else it
            }
        }
    }

    override suspend fun setTyping(conversationId: ConversationId, isTyping: Boolean) {
        // not called from the stub
    }

    override suspend fun deleteMessage(conversationId: ConversationId, messageId: MessageId) {
        flowMutex.withLock {
            messagesFlowPerConversation[conversationId]!!.value = messagesOf(conversationId).map {
                if (it.baseMessage.id == messageId) {
                    Message.Deleted(it.baseMessage)
                } else it
            }
        }
    }

    private fun messagesOf(conversationId: ConversationId) =
        messagesFlowPerConversation[conversationId]!!.value

    private fun conversationItemsStateFlow(prepopulate: Boolean): MutableStateFlow<List<Message>> = MutableStateFlow(
        buildList {
            if (prepopulate) {
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
        }
    )

    companion object {
        private const val MOCK_ERRORS = false
    }
}
