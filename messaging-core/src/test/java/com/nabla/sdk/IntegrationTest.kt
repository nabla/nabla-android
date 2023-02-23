package com.nabla.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.NetworkConfiguration
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.data.stubs.StdLogger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.DeletedProvider
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.NablaMessagingModule
import com.nabla.sdk.messaging.core.data.stubs.GqlData
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivityContent
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.FileLocal
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation.Companion.TYPING_TIME_WINDOW
import com.nabla.sdk.messaging.core.domain.entity.ProviderNotFoundException
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.core.messagingClient
import com.nabla.sdk.messaging.graphql.ConversationEventsSubscription
import com.nabla.sdk.messaging.graphql.ConversationsEventsSubscription
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.StableRandomIdGenerator
import com.nabla.sdk.tests.common.apollo.FlowTestNetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okreplay.MatchRules
import okreplay.OkReplay
import okreplay.OkReplayConfig
import okreplay.OkReplayInterceptor
import okreplay.RecorderRule
import okreplay.TapeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class IntegrationTest : BaseCoroutineTest() {
    private val refreshToken: String
    private val accessToken: String
    private val tapeMode: TapeMode

    init {
        CoreContainer.overriddenOkHttpClient = {
            // Add replay interceptor in first to redact auth tokens from tape
            it.interceptors().add(0, okReplayInterceptor)
        }
        val properties = Properties()
        tapeMode = try {
            try {
                properties.load(FileReader(File(TEST_CONFIG_OVERRIDE_PATH)))
            } catch (e: FileNotFoundException) {
                throw ReplayException
            }

            if (properties.getProperty("ignore", "false") == "true") {
                throw ReplayException
            }

            println("--- RECORD ---")
            TapeMode.WRITE_ONLY
        } catch (throwable: ReplayException) {
            println("--- REPLAY ---")
            println(
                "If you want to record new integration test, please create a" +
                    " $TEST_CONFIG_OVERRIDE_PATH file with a valid `baseUrl`, `accessToken`" +
                    " and `refreshToken`"
            )
            TapeMode.READ_ONLY
        }
        refreshToken = properties.getProperty("refreshToken", DUMMY_TOKEN)
        accessToken = properties.getProperty("accessToken", DUMMY_TOKEN)
    }

    private object ReplayException : Exception()

    private val okReplayInterceptor = OkReplayInterceptor()

    @Rule
    @JvmField
    val recorder = RecorderRule(
        OkReplayConfig.Builder()
            .interceptor(okReplayInterceptor)
            .defaultMode(tapeMode)
            .defaultMatchRules(
                MatchRules.method,
                MatchRules.path,
                MatchRules.body
            ).build()
    )

    @Test
    @OkReplay
    fun `test conversation creation and conversations & conversation watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000000"), tapeMode),
        )

        val replayEventEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            replayEventEmitter
        )

        nablaMessagingClient.watchConversations().test {
            val firstPageOfConversationResponse = awaitItem()
            val messageToSend = MessageInput.Text("Hello")
            val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

            replayEventEmitter.emit(GqlData.ConversationsEvents.conversationCreated(createdConversation.id))

            val updatedPageOfConversationsResponse = awaitItem()

            val expectedContentOfUpdatedPageOfConversations = listOf(createdConversation) + firstPageOfConversationResponse.data.content
            assertEquals(
                expectedContentOfUpdatedPageOfConversations.map { it.id },
                updatedPageOfConversationsResponse.data.content.map { it.id }
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation creation with custom title`() = runTest {
        val (nablaMessagingClient) = setupClient(
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000001"), tapeMode),
        )

        val title = "Test custom title"
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend, title = title).getOrThrow()
        assertEquals(title, createdConversation.title)
    }

    @Test
    @OkReplay
    fun `test conversation creation with unknown provider`() = runTest {
        val (nablaMessagingClient) = setupClient(
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000002"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")

        assertFailsWith<ProviderNotFoundException> {
            nablaMessagingClient.createConversationWithMessage(message = messageToSend, providerIds = listOf(Uuid.fromString("01234567-0000-0000-0000-000000000000"))).getOrThrow()
        }
    }

    @Test
    @OkReplay
    fun `test conversation text message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                // In the future so that the order of messages doesn't change when they will change from local to remote
                override fun now(): Instant = Instant.parse("2040-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000003"), tapeMode),
        )

        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(1, firstEmit.data.content.size)
            assertNull(firstEmit.data.loadMore)

            val messageContent = "Hello 2"
            nablaMessagingClient.sendMessage(MessageInput.Text(messageContent), createdConversation.id).getOrThrow()

            val secondEmit = awaitItem()
            val message = secondEmit.data.content.first()
            assertIs<Message.Text>(message)
            assertEquals(messageContent, message.text)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message.author)
            assertEquals(secondEmit.data.content.size, 2)
            assertNull(secondEmit.data.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.data.content.first()
            assertIs<Message.Text>(message2)
            assertEquals(messageContent, message2.text)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message2.author)
            assertEquals(thirdEmit.data.content.size, 2)
            assertNull(thirdEmit.data.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientTextMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.data.content.first()
            assertIs<Message.Text>(message3)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertIs<MessageAuthor.Patient>(message3.author)
            assertEquals(lastEmit.data.content.size, 2)
            assertNull(lastEmit.data.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation image message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                // In the future so that the order of messages doesn't change when they will change from local to remote
                override fun now(): Instant = Instant.parse("2040-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000004"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(1, firstEmit.data.content.size)
            assertNull(firstEmit.data.loadMore)

            val uri = Uri("content://image_test")
            setupContentProviderForMediaUpload(uri)

            val mediaSource = FileSource.Local<FileLocal.Image, FileUpload.Image>(
                FileLocal.Image(
                    uri = uri,
                    fileName = null,
                    mimeType = MimeType.Image.Jpeg,
                )
            )

            nablaMessagingClient.sendMessage(
                MessageInput.Media.Image(mediaSource),
                createdConversation.id
            ).getOrThrow()

            val secondEmit = awaitItem()
            val message = secondEmit.data.content.first()
            assertIs<Message.Media.Image>(message)
            assertEquals(mediaSource, message.mediaSource)
            assertEquals(uri, message.stableUri)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message.author)
            assertEquals(secondEmit.data.content.size, 2)
            assertNull(secondEmit.data.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.data.content.first()
            assertIs<Message.Media.Image>(message2)
            assertEquals(mediaSource, message2.mediaSource)
            assertEquals(uri, message2.stableUri)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message2.author)
            assertEquals(thirdEmit.data.content.size, 2)
            assertNull(thirdEmit.data.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientImageMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.data.content.first()
            assertIs<Message.Media.Image>(message3)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertIs<MessageAuthor.Patient>(message3.author)
            assertEquals(lastEmit.data.content.size, 2)
            assertNull(lastEmit.data.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation document message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                // In the future so that the order of messages doesn't change when they will change from local to remote
                override fun now(): Instant = Instant.parse("2040-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000005"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(1, firstEmit.data.content.size)
            assertNull(firstEmit.data.loadMore)

            val uri = Uri("content://document_test")
            setupContentProviderForMediaUpload(uri)

            val docName = "test.pdf"
            val mimeType = MimeType.Application.Pdf
            val mediaSource = FileSource.Local<FileLocal.Document, FileUpload.Document>(
                FileLocal.Document(
                    uri = uri,
                    fileName = docName,
                    mimeType = mimeType,
                )
            )

            nablaMessagingClient.sendMessage(
                MessageInput.Media.Document(mediaSource),
                createdConversation.id
            ).getOrThrow()

            val secondEmit = awaitItem()
            val message = secondEmit.data.content.first()
            assertIs<Message.Media.Document>(message)
            assertEquals(mediaSource, message.mediaSource)
            assertEquals(uri, message.stableUri)
            assertEquals(mimeType, message.mimeType)
            assertEquals(docName, message.fileName)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message.author)
            assertEquals(secondEmit.data.content.size, 2)
            assertNull(secondEmit.data.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.data.content.first()
            assertIs<Message.Media.Document>(message2)
            assertEquals(mediaSource, message2.mediaSource)
            assertEquals(uri, message2.stableUri)
            assertEquals(mimeType, message2.mimeType)
            assertEquals(docName, message2.fileName)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient.Current, message2.author)
            assertEquals(thirdEmit.data.content.size, 2)
            assertNull(thirdEmit.data.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientDocumentMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.data.content.first()
            assertIs<Message.Media.Document>(message3)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertIs<MessageAuthor.Patient>(message3.author)
            assertEquals(lastEmit.data.content.size, 2)
            assertNull(lastEmit.data.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation local text message deletion watching`() = runTest {
        val (nablaMessagingClient) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000006"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            awaitItem() // First emit with empty conversation

            val messageContent = "Hello"
            val messageId = nablaMessagingClient.sendMessage(MessageInput.Text(messageContent), createdConversation.id).getOrThrow()

            val firstEmit = awaitItem()
            val message = firstEmit.data.content.first()
            assertIs<Message.Text>(message)
            assertEquals(messageContent, message.text)

            awaitItem() // Another emit with message marked as sent

            nablaMessagingClient.deleteMessage(createdConversation.id, messageId).getOrThrow()

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.data.content.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test watch conversation is called when the providers are updated`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000007"), tapeMode),
        )

        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.data.providersInConversation.size)

            val providerId = uuid4()

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.providerJoinsConversation(
                    conversationId = createdConversation.id,
                    providerId = providerId,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.data.providersInConversation.size)
            assertEquals(providerId, secondEmit.data.providersInConversation.first().provider.id)

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.providersLeaveConversation(
                    conversationId = createdConversation.id,
                )
            )

            val thirdEmit = awaitItem()
            assertEquals(0, thirdEmit.data.providersInConversation.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test new existing provider in conversation activity watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000008"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(1, firstEmit.data.content.size)

            val providerId = uuid4()
            replayEventEmitter.emit(
                GqlData.ConversationEvents.Activity.existingProviderJoinedActivity(
                    conversationId = createdConversation.id,
                    providerId = providerId,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(2, secondEmit.data.content.size)
            val activityItem = secondEmit.data.content.first()
            assertIs<ConversationActivity>(activityItem)
            val activityContent = activityItem.content
            assertIs<ConversationActivityContent.ProviderJoinedConversation>(activityContent)
            val provider = activityContent.maybeProvider
            assertIs<Provider>(provider)
            assertEquals(providerId, provider.id)
        }
    }

    @Test
    @OkReplay
    fun `test marking a conversation as read updates the conversation`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000009"), tapeMode),
        )

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()
        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            assertEquals(0, awaitItem().data.patientUnreadMessageCount)

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.conversationUpdatedForPatientUnreadMessageCount(
                    conversationId = createdConversation.id,
                    patientUnreadMessageCount = 1,
                )
            )

            assertEquals(1, awaitItem().data.patientUnreadMessageCount)

            nablaMessagingClient.markConversationAsRead(createdConversation.id).getOrThrow()

            assertEquals(0, awaitItem().data.patientUnreadMessageCount)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test provider typing events are updating the conversation`() = runTest {
        val now = Instant.parse("2020-01-01T00:00:00Z")
        val clock = object : Clock {
            override fun now(): Instant = now
        }
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = clock,
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000010"), tapeMode),
        )

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            assertEquals(0, awaitItem().data.providersInConversation.size)

            val providerId = uuid4()
            val providerInConversationId = uuid4()

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.providerJoinsConversation(
                    conversationId = createdConversation.id,
                    providerInConversationId = providerInConversationId,
                    providerId = providerId,
                    providerIsTypingAt = null,
                )
            )

            assertFalse(awaitItem().data.providersInConversation.first().isTyping(clock))

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = now.minus(1.seconds),
                )
            )

            assertTrue(awaitItem().data.providersInConversation.first().isTyping(clock))
            awaitItem() // We emit twice as the delay when provider isn't typing anymore is executed instantaneously in tests

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = now.minus(TYPING_TIME_WINDOW),
                )
            )

            assertFalse(awaitItem().data.providersInConversation.first().isTyping(clock))
            awaitItem() // We emit twice as the delay when provider isn't typing anymore is executed instantaneously in tests

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = null,
                )
            )

            assertFalse(awaitItem().data.providersInConversation.first().isTyping(clock))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test new deleted provider in conversation activity watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = StableRandomIdGenerator(idFile("00000000-0000-0000-0000-000000000011"), tapeMode),
        )
        val messageToSend = MessageInput.Text("Hello")
        val createdConversation = nablaMessagingClient.createConversationWithMessage(message = messageToSend).getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.stableId),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(1, firstEmit.data.content.size)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Activity.deletedProviderJoinedActivity(
                    conversationId = createdConversation.id,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(2, secondEmit.data.content.size)
            val activityItem = secondEmit.data.content.first()
            assertIs<ConversationActivity>(activityItem)
            val activityContent = activityItem.content
            assertIs<ConversationActivityContent.ProviderJoinedConversation>(activityContent)
            val provider = activityContent.maybeProvider
            assertIs<DeletedProvider>(provider)
        }
    }

    @Test
    fun `test calling public methods without auth throws`() = runTest {
        val (nablaMessagingClient) = setupClient(
            setCurrentUser = false,
        )

        nablaMessagingClient.watchConversations().test {
            assertEquals(AuthenticationException.UserIdNotSet, awaitError())
        }

        nablaMessagingClient.watchConversation(ConversationId.Remote(remoteId = uuid4())).test {
            assertEquals(AuthenticationException.UserIdNotSet, awaitError())
        }

        nablaMessagingClient.watchConversationItems(ConversationId.Remote(remoteId = uuid4())).test {
            assertEquals(AuthenticationException.UserIdNotSet, awaitError())
        }

        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.createConversationWithMessage(message = MessageInput.Text("Hello"))
                .exceptionOrNull()
        )
        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.deleteMessage(
                ConversationId.Remote(remoteId = uuid4()),
                MessageId.Local(uuid4()),
            ).exceptionOrNull()
        )
        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.markConversationAsRead(
                ConversationId.Remote(remoteId = uuid4())
            ).exceptionOrNull()
        )
        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.retrySendingMessage(
                MessageId.Local(uuid4()),
                ConversationId.Remote(remoteId = uuid4()),
            ).exceptionOrNull()
        )
        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.setTyping(
                ConversationId.Remote(remoteId = uuid4()),
                isTyping = true,
            ).exceptionOrNull()
        )
        assertEquals(
            AuthenticationException.UserIdNotSet,
            nablaMessagingClient.sendMessage(
                MessageInput.Text(""),
                ConversationId.Remote(remoteId = uuid4()),
            ).exceptionOrNull()
        )
    }

    private fun setupContentProviderForMediaUpload(uri: Uri) {
        Shadows.shadowOf(RuntimeEnvironment.getApplication().contentResolver).registerInputStream(
            uri.toAndroidUri(),
            ByteArrayInputStream(byteArrayOf()),
        )
    }

    private fun setupClient(
        clock: Clock? = null,
        uuidGenerator: UuidGenerator? = null,
        setCurrentUser: Boolean = true,
    ): ComponentUnderTest {
        CoreContainer.overriddenUuidGenerator = uuidGenerator
        CoreContainer.overriddenClock = clock
        val mockSubscriptionNetworkTransport = FlowTestNetworkTransport()

        CoreContainer.overriddenApolloWsConfig = {
            it.subscriptionNetworkTransport(mockSubscriptionNetworkTransport)
        }

        val nablaClient = NablaClient.initialize(
            name = uuid4().toString(),
            configuration = Configuration(
                context = RuntimeEnvironment.getApplication(),
                publicApiKey = "dummy-api-key",
                logger = StdLogger(),
            ),
            networkConfiguration = NetworkConfiguration(
                baseUrl = baseUrl,
            ),
            modules = listOf(NablaMessagingModule()),
            sessionTokenProvider = {
                Result.success(
                    AuthTokens(
                        refreshToken = refreshToken,
                        accessToken = accessToken
                    )
                )
            }
        )

        if (setCurrentUser) {
            nablaClient.setCurrentUserOrThrow("dummy-user")
        }

        return ComponentUnderTest(nablaClient.messagingClient, mockSubscriptionNetworkTransport)
    }

    companion object {
        private const val baseUrl = "http://localhost:8080/"

        // Valid JWT token from https://www.javainuse.com/jwtgenerator
        private const val DUMMY_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY1NTAzNTc1NCwiaWF0IjoxNjUyMzU3MzU0fQ.2cwXmXR_yYZHojuKHLqDAykPCPQCFJ1pEOsoKn_UeaA"
        private const val TEST_CONFIG_OVERRIDE_PATH = "src/test/record.properties"

        private fun idFile(key: String) = File("src/test/resources/stable-random-ids/$key.txt")
    }
}

private data class ComponentUnderTest(
    val nablaMessagingClient: NablaMessagingClient,
    val mockSubscriptionNetworkTransport: FlowTestNetworkTransport,
)
