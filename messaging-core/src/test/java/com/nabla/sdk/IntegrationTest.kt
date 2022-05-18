package com.nabla.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.data.logger.StdLogger
import com.nabla.sdk.core.domain.boundary.UuidGenerator
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageInput
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.coroutines.flow.first
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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            properties.load(FileReader(File(TEST_CONFIG_OVERRIDE_PATH)))
            println("--- RECORD ---")
            TapeMode.WRITE_ONLY
        } catch (throwable: FileNotFoundException) {
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
        val nablaMessagingClient = setupClient()
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()
        val firstPageOfConversations = nablaMessagingClient.watchConversations().first()

        // We just created a conversation, it should not be empty
        assertTrue(firstPageOfConversations.content.isNotEmpty())

        val watchedConversation =
            nablaMessagingClient.watchConversation(createdConversation.id).first()
        assertEquals(createdConversation.id, watchedConversation.id)
    }

    @Test
    @OkReplay
    fun `test conversation text message sending watching`() = runTest {
        val nablaMessagingClient = setupClient(
            clock = object : Clock {
                override fun now(): Instant = Instant.parse("2020-01-01T00:00:00Z")
            },
            uuidGenerator = object : UuidGenerator {
                override fun generate(): Uuid = Uuid.fromString("00000000-0000-0000-0000-000000000000")
            }
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
            assertEquals(MessageSender.Patient, message.sender)
            assertEquals(createdConversation.id, secondEmit.content.conversationId)
            assertNull(secondEmit.loadMore)

            val thirdEmit = awaitItem()
            val message2 = thirdEmit.content.items.first()
            assertIs<Message.Text>(message2)
            assertEquals("Hello", message2.text)
            assertEquals(createdConversation.id, message2.conversationId)
            assertIs<MessageId.Local>(message2.id)
            assertEquals(SendStatus.Sent, message2.sendStatus)
            assertEquals(MessageSender.Patient, message2.sender)
            assertEquals(createdConversation.id, thirdEmit.content.conversationId)
            assertNull(thirdEmit.loadMore)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun setupClient(
        clock: Clock? = null,
        uuidGenerator: UuidGenerator? = null,
    ): NablaMessagingClient {
        CoreContainer.overriddenUuidGenerator = uuidGenerator
        CoreContainer.overriddenClock = clock

        val nablaClient = NablaClient.initialize(
            name = Uuid.randomUUID().toString(),
            configuration = Configuration(
                context = RuntimeEnvironment.getApplication(),
                publicApiKey = "dummy-api-key",
                baseUrl = baseUrl,
                isLoggingEnabled = true
            )
        )
        val nablaMessagingClient = NablaMessagingClient.initialize(nablaClient)
        nablaClient.authenticate("dummy-user") {
            Result.success(
                AuthTokens(
                    refreshToken = refreshToken,
                    accessToken = accessToken
                )
            )
        }
        return nablaMessagingClient
    }

    companion object {
        private const val DUMMY_BASE_URL = "https://dummy-base-url/api/"

        // Valid JWT token from https://www.javainuse.com/jwtgenerator
        private const val DUMMY_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY1NTAzNTc1NCwiaWF0IjoxNjUyMzU3MzU0fQ.2cwXmXR_yYZHojuKHLqDAykPCPQCFJ1pEOsoKn_UeaA"
        private const val TEST_CONFIG_OVERRIDE_PATH = "src/test/record.properties"
    }
}
