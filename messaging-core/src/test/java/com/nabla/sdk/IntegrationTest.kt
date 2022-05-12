package com.nabla.sdk

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.data.logger.StdLogger
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.NablaMessagingClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
    fun `test conversations`() = runTest {
        val nablaMessagingClient = setupClient()
        val createdConversation = nablaMessagingClient.createConversation().getOrThrow()
        val firstPageOfConversations = nablaMessagingClient.watchConversations().first()

        // We just created a conversation, it should not be empty
        assertTrue(firstPageOfConversations.content.isNotEmpty())

        val watchedConversation =
            nablaMessagingClient.watchConversation(createdConversation.id).first()
        assertEquals(createdConversation.id, watchedConversation.id)
    }

    private fun setupClient(): NablaMessagingClient {
        val nablaClient = NablaClient.initialize(
            Configuration(
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
        private const val DUMMY_BASE_URL = "https://dummy-base-url/"

        // Valid JWT token from https://www.javainuse.com/jwtgenerator
        private const val DUMMY_TOKEN =
            "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiQWRtaW4iLCJJc3N1ZXIiOiJJc3N1ZXIiLCJVc2VybmFtZSI6IkphdmFJblVzZSIsImV4cCI6MTY1NTAzNTc1NCwiaWF0IjoxNjUyMzU3MzU0fQ.2cwXmXR_yYZHojuKHLqDAykPCPQCFJ1pEOsoKn_UeaA"
        private const val TEST_CONFIG_OVERRIDE_PATH = "src/test/record.properties"
    }
}
