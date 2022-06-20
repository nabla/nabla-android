package com.nabla.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.NetworkConfiguration
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.data.logger.StdLogger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.DeletedProvider
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.messaging.core.NablaMessagingClient
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
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.test.apollo.FlowTestNetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
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
internal class IntegrationTest {

    private val baseUrl: String
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
        baseUrl = properties.getProperty("baseUrl", DUMMY_BASE_URL)
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
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient()

        val replayEventEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            replayEventEmitter
        )

        nablaMessagingClient.watchConversations().test {
            val firstPageOfConversation = awaitItem()
            val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

            replayEventEmitter.emit(GqlData.ConversationsEvents.conversationCreated(createdConversation.id))

            val updatedPageOfConversations = awaitItem()

            val expectedContentOfUpdatedPageOfConversations = listOf(createdConversation) + firstPageOfConversation.content
            assertEquals(
                expectedContentOfUpdatedPageOfConversations.map { it.id },
                updatedPageOfConversations.content.map { it.id }
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation creation with custom title`() = runTest {
        val (nablaMessagingClient) = setupClient()

        val title = "Test custom title"
        val createdConversation = nablaMessagingClient.createConversation(title = title).getOrThrow()
        assertEquals(title, createdConversation.title)
    }

    @Test
    @OkReplay
    fun `test conversation creation with unknown provider`() = runTest {
        val (nablaMessagingClient) = setupClient()

        assertFailsWith<NablaException.ProviderNotFound> {
            nablaMessagingClient.createConversation(providerIdToAssign = Uuid.fromString("01234567-0000-0000-0000-000000000000")).getOrThrow()
        }
    }

    @Test
    @OkReplay
    fun `test conversation text message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000000")
            },
        )

        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            assertEquals(createdConversation.id, firstEmit.content.conversationId)
            assertNull(firstEmit.loadMore)

            val messageToSend = "Hello"
            nablaMessagingClient.sendMessage(MessageInput.Text(messageToSend), createdConversation.id).getOrThrow()

            val secondEmit = awaitItem()
            val message = secondEmit.content.items.first()
            assertIs<Message.Text>(message)
            assertEquals("Hello", message.text)
            assertEquals(createdConversation.id, message.conversationId)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient, message.author)
            assertEquals(createdConversation.id, secondEmit.content.conversationId)
            assertEquals(secondEmit.content.items.size, 1)
            assertNull(secondEmit.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.content.items.first()
            assertIs<Message.Text>(message2)
            assertEquals("Hello", message2.text)
            assertEquals(createdConversation.id, message2.conversationId)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient, message2.author)
            assertEquals(createdConversation.id, thirdEmit.content.conversationId)
            assertEquals(thirdEmit.content.items.size, 1)
            assertNull(thirdEmit.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientTextMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.content.items.first()
            assertIs<Message.Text>(message3)
            assertEquals(createdConversation.id, message3.conversationId)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertEquals(MessageAuthor.Patient, message3.author)
            assertEquals(createdConversation.id, lastEmit.content.conversationId)
            assertEquals(lastEmit.content.items.size, 1)
            assertNull(lastEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation image message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000001")
            },
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            assertEquals(createdConversation.id, firstEmit.content.conversationId)
            assertNull(firstEmit.loadMore)

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
            val message = secondEmit.content.items.first()
            assertIs<Message.Media.Image>(message)
            assertEquals(mediaSource, message.mediaSource)
            assertEquals(uri, message.stableUri)
            assertEquals(createdConversation.id, message.conversationId)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient, message.author)
            assertEquals(createdConversation.id, secondEmit.content.conversationId)
            assertEquals(secondEmit.content.items.size, 1)
            assertNull(secondEmit.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.content.items.first()
            assertIs<Message.Media.Image>(message2)
            assertEquals(mediaSource, message2.mediaSource)
            assertEquals(uri, message2.stableUri)
            assertEquals(createdConversation.id, message2.conversationId)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient, message2.author)
            assertEquals(createdConversation.id, thirdEmit.content.conversationId)
            assertEquals(thirdEmit.content.items.size, 1)
            assertNull(thirdEmit.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientImageMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.content.items.first()
            assertIs<Message.Media.Image>(message3)
            assertEquals(createdConversation.id, message3.conversationId)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertEquals(MessageAuthor.Patient, message3.author)
            assertEquals(createdConversation.id, lastEmit.content.conversationId)
            assertEquals(lastEmit.content.items.size, 1)
            assertNull(lastEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation document message sending watching`() = runTest {
        val (nablaMessagingClient, replaySubscriptionNetworkTransport) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000002")
            },
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            assertEquals(createdConversation.id, firstEmit.content.conversationId)
            assertNull(firstEmit.loadMore)

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
            val message = secondEmit.content.items.first()
            assertIs<Message.Media.Document>(message)
            assertEquals(mediaSource, message.mediaSource)
            assertEquals(uri, message.stableUri)
            assertEquals(mimeType, message.mimeType)
            assertEquals(docName, message.fileName)
            assertEquals(createdConversation.id, message.conversationId)
            assertIs<MessageId.Local>(message.id)
            assertEquals(SendStatus.Sending, message.sendStatus)
            assertEquals(MessageAuthor.Patient, message.author)
            assertEquals(createdConversation.id, secondEmit.content.conversationId)
            assertEquals(secondEmit.content.items.size, 1)
            assertNull(secondEmit.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.content.items.first()
            assertIs<Message.Media.Document>(message2)
            assertEquals(mediaSource, message2.mediaSource)
            assertEquals(uri, message2.stableUri)
            assertEquals(mimeType, message2.mimeType)
            assertEquals(docName, message2.fileName)
            assertEquals(createdConversation.id, message2.conversationId)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageAuthor.Patient, message2.author)
            assertEquals(createdConversation.id, thirdEmit.content.conversationId)
            assertEquals(thirdEmit.content.items.size, 1)
            assertNull(thirdEmit.loadMore)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.MessageCreated.patientDocumentMessage(
                    conversationId = createdConversation.id,
                    localMessageId = message2.id,
                )
            )

            val lastEmit = awaitItem()
            val message3 = lastEmit.content.items.first()
            assertIs<Message.Media.Document>(message3)
            assertEquals(createdConversation.id, message3.conversationId)
            assertIs<MessageId.Remote>(message3.id)
            assertEquals(message2.id.clientId, message3.id.clientId)
            assertEquals(SendStatus.Sent, message3.sendStatus)
            assertEquals(MessageAuthor.Patient, message3.author)
            assertEquals(createdConversation.id, lastEmit.content.conversationId)
            assertEquals(lastEmit.content.items.size, 1)
            assertNull(lastEmit.loadMore)

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
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000003")
            },
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            awaitItem() // First emit with empty conversation

            val messageToSend = "Hello"
            val messageId = nablaMessagingClient.sendMessage(MessageInput.Text(messageToSend), createdConversation.id).getOrThrow()

            val firstEmit = awaitItem()
            val message = firstEmit.content.items.first()
            assertIs<Message.Text>(message)
            assertEquals("Hello", message.text)

            awaitItem() // Another emit with message marked as sent

            nablaMessagingClient.deleteMessage(createdConversation.id, messageId).getOrThrow()

            val secondEmit = awaitItem()
            assertEquals(0, secondEmit.content.items.size)

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
        )

        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.providersInConversation.size)

            val providerId = uuid4()

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.providerJoinsConversation(
                    conversationId = createdConversation.id,
                    providerId = providerId,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.providersInConversation.size)
            assertEquals(providerId, secondEmit.providersInConversation.first().provider.id)

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.providersLeaveConversation(
                    conversationId = createdConversation.id,
                )
            )

            val thirdEmit = awaitItem()
            assertEquals(0, thirdEmit.providersInConversation.size)

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
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            val providerId = uuid4()
            replayEventEmitter.emit(
                GqlData.ConversationEvents.Activity.existingProviderJoinedActivity(
                    conversationId = createdConversation.id,
                    providerId = providerId,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.content.items.size)
            val activityItem = secondEmit.content.items.first()
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
        )

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()
        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.patientUnreadMessageCount)

            conversationsReplayEmitter.emit(
                GqlData.ConversationsEvents.conversationUpdated(
                    conversationId = createdConversation.id,
                    patientUnreadMessageCount = 1,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.patientUnreadMessageCount)

            nablaMessagingClient.markConversationAsRead(createdConversation.id).getOrThrow()

            val third = awaitItem()
            assertEquals(0, third.patientUnreadMessageCount)

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
        )

        val conversationsReplayEmitter = MutableSharedFlow<ConversationsEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            conversationsReplayEmitter
        )

        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val conversationUpdatesFlow = nablaMessagingClient.watchConversation(createdConversation.id)

        conversationUpdatesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.providersInConversation.size)

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

            val secondEmit = awaitItem()
            assertFalse(secondEmit.providersInConversation.first().isTyping(clock))

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = now.minus(1.seconds),
                )
            )

            val thirdEmit = awaitItem()
            assertTrue(thirdEmit.providersInConversation.first().isTyping(clock))

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = now.minus(TYPING_TIME_WINDOW),
                )
            )

            val fourthEmit = awaitItem()
            assertFalse(fourthEmit.providersInConversation.first().isTyping(clock))

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Typing.providerIsTyping(
                    providerId = providerId,
                    providerInConversationId = providerInConversationId,
                    typingAtInstant = null,
                )
            )

            val fifthEmit = awaitItem()
            assertFalse(fifthEmit.providersInConversation.first().isTyping(clock))

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
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val replayEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()
        replaySubscriptionNetworkTransport.register(
            ConversationEventsSubscription(createdConversation.id.value),
            replayEventEmitter
        )

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            replayEventEmitter.emit(
                GqlData.ConversationEvents.Activity.deletedProviderJoinedActivity(
                    conversationId = createdConversation.id,
                )
            )

            val secondEmit = awaitItem()
            assertEquals(1, secondEmit.content.items.size)
            val activityItem = secondEmit.content.items.first()
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
            authenticate = false,
        )

        nablaMessagingClient.watchConversations().test {
            assertEquals(NablaException.Authentication.NotAuthenticated, awaitError())
        }

        nablaMessagingClient.watchConversation(ConversationId(Uuid.randomUUID())).test {
            assertEquals(NablaException.Authentication.NotAuthenticated, awaitError())
        }

        nablaMessagingClient.watchConversationItems(ConversationId(Uuid.randomUUID())).test {
            assertEquals(NablaException.Authentication.NotAuthenticated, awaitError())
        }

        assertEquals(NablaException.Authentication.NotAuthenticated, nablaMessagingClient.createConversation().exceptionOrNull())
        assertEquals(
            NablaException.Authentication.NotAuthenticated,
            nablaMessagingClient.deleteMessage(
                ConversationId(Uuid.randomUUID()),
                MessageId.Local(Uuid.randomUUID()),
            ).exceptionOrNull()
        )
        assertEquals(
            NablaException.Authentication.NotAuthenticated,
            nablaMessagingClient.markConversationAsRead(
                ConversationId(Uuid.randomUUID())
            ).exceptionOrNull()
        )
        assertEquals(
            NablaException.Authentication.NotAuthenticated,
            nablaMessagingClient.retrySendingMessage(
                MessageId.Local(Uuid.randomUUID()),
                ConversationId(Uuid.randomUUID()),
            ).exceptionOrNull()
        )
        assertEquals(
            NablaException.Authentication.NotAuthenticated,
            nablaMessagingClient.setTyping(
                ConversationId(Uuid.randomUUID()),
                isTyping = true,
            ).exceptionOrNull()
        )
        assertEquals(
            NablaException.Authentication.NotAuthenticated,
            nablaMessagingClient.sendMessage(
                MessageInput.Text(""),
                ConversationId(Uuid.randomUUID()),
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
        authenticate: Boolean = true,
    ): ComponentUnderTest {
        CoreContainer.overriddenUuidGenerator = uuidGenerator
        CoreContainer.overriddenClock = clock
        val mockSubscriptionNetworkTransport = FlowTestNetworkTransport()

        CoreContainer.overriddenApolloWsConfig = {
            it.subscriptionNetworkTransport(mockSubscriptionNetworkTransport)
        }

        val nablaClient = NablaClient.initialize(
            name = Uuid.randomUUID().toString(),
            configuration = Configuration(
                context = RuntimeEnvironment.getApplication(),
                publicApiKey = "dummy-api-key",
                logger = StdLogger(),
            ),
            networkConfiguration = NetworkConfiguration(
                baseUrl = baseUrl,
            )
        )

        val nablaMessagingClient = NablaMessagingClient.initialize(nablaClient)

        if (authenticate) {
            nablaClient.authenticate("dummy-user") {
                Result.success(
                    AuthTokens(
                        refreshToken = refreshToken,
                        accessToken = accessToken
                    )
                )
            }
        }

        return ComponentUnderTest(nablaMessagingClient, mockSubscriptionNetworkTransport)
    }

    companion object {
        private const val DUMMY_BASE_URL = "https://dummy-base-url/api/"

        // Valid JWT token from https://www.javainuse.com/jwtgenerator
        private const val DUMMY_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY1NTAzNTc1NCwiaWF0IjoxNjUyMzU3MzU0fQ.2cwXmXR_yYZHojuKHLqDAykPCPQCFJ1pEOsoKn_UeaA"
        private const val TEST_CONFIG_OVERRIDE_PATH = "src/test/record.properties"
    }
}

private data class ComponentUnderTest(
    val nablaMessagingClient: NablaMessagingClient,
    val mockSubscriptionNetworkTransport: FlowTestNetworkTransport,
)
