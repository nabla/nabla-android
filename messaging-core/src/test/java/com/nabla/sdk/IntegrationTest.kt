package com.nabla.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.data.logger.StdLogger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.graphql.ConversationsEventsSubscription
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.data.stubs.GqlData
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.FileLocal
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.test.apollo.FlowTestNetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
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
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class IntegrationTest {

    private val baseUrl: String
    private val refreshToken: String
    private val accessToken: String
    private val tapeMode: TapeMode

    init {
        CoreContainer.overriddenLogger = StdLogger()
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

        val replayEventEmitter = MutableStateFlow<ConversationsEventsSubscription.Data?>(null)
        replaySubscriptionNetworkTransport.register(
            ConversationsEventsSubscription(),
            replayEventEmitter.filterNotNull()
        )

        nablaMessagingClient.watchConversations().test {
            val firstPageOfConversation = awaitItem()
            val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

            replayEventEmitter.value = GqlData.ConversationsEvents.conversationCreated(createdConversation.id)

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
    fun `test conversation text message sending watching`() = runTest {
        val (nablaMessagingClient) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000000")
            },
            disposeSubscriptionNetworkTransport = true
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

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
            assertNull(thirdEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation image message sending watching`() = runTest {
        val (nablaMessagingClient) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000001")
            },
            disposeSubscriptionNetworkTransport = true
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

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
                    mimeType = MimeType.Image.JPEG,
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
            assertNull(thirdEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation document message sending watching`() = runTest {
        val (nablaMessagingClient) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000002")
            },
            disposeSubscriptionNetworkTransport = true
        )
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()

        val messagesFlow = nablaMessagingClient.watchConversationItems(createdConversation.id)

        messagesFlow.test {
            val firstEmit = awaitItem()
            assertEquals(0, firstEmit.content.items.size)

            assertEquals(createdConversation.id, firstEmit.content.conversationId)
            assertNull(firstEmit.loadMore)

            val uri = Uri("content://document_test")
            setupContentProviderForMediaUpload(uri)

            val docName = "test.pdf"
            val mimeType = MimeType.Application.PDF
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
            assertNull(thirdEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @OkReplay
    fun `test conversation text message deletion watching`() = runTest {
        val (nablaMessagingClient) = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000003")
            },
            disposeSubscriptionNetworkTransport = true
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
    fun `test calling public methods without auth throws`() = runTest {
        val (nablaMessagingClient) = setupClient(
            authenticate = false,
            disposeSubscriptionNetworkTransport = true
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
        disposeSubscriptionNetworkTransport: Boolean = false,
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
                baseUrl = baseUrl,
                isLoggingEnabled = true
            )
        )

        // Disable websocket connection for tests as events are not recorded
        if (disposeSubscriptionNetworkTransport) {
            nablaClient.coreContainer.apolloClient.subscriptionNetworkTransport.dispose()
        }

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
