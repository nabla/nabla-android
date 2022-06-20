package com.nabla.sdk.core.data.apollo

import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.messaging.core.data.stubs.fake
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.test.TestClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.UnknownHostException
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionExtKtTest {

    @Test
    fun `retryOnNetworkErrorWithExponentialBackoff() retries on network error`() = runTest {
        val gqlFlow = gqlFlowThatThrowsNetworkErrorThenEmitItem().retryOnNetworkErrorWithExponentialBackoff()
        gqlFlow.test {
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun `retryOnNetworkErrorAndShareIn() retries on network error`() = runTest {
        val job = Job()
        val gqlFlow = gqlFlowThatThrowsNetworkErrorThenEmitItem().retryOnNetworkErrorAndShareIn(this + job)
        gqlFlow.test {
            awaitItem()
        }
        job.cancel()
    }

    @Test
    fun `retryOnNetworkErrorAndShareIn() propagate to downstream not network error`() = runTest {
        val job = Job()
        val gqlFlow = flow<ApolloResponse<Operation.Data>> {
            throw IllegalArgumentException()
        }.retryOnNetworkErrorAndShareIn(this + job)
        gqlFlow.test {
            awaitError()
        }
        job.cancel()
    }

    @Test
    fun `notifyTypingUpdates() notifies typing updates`() = runTest {
        val clock = TestClock(this)
        val typingProvider = ProviderInConversation(
            Provider.fake(), clock.now(), null
        )
        val eventFlowWithProviders = flow {
            emit(typingProvider)
        }.notifyTypingUpdates(clock, coroutineContext) {
            listOf(it)
        }
        eventFlowWithProviders.test {
            assertTrue(awaitItem().isTyping(clock))
            assertFalse(awaitItem().isTyping(clock))
            awaitComplete()
        }
    }

    private fun gqlFlowThatThrowsNetworkErrorThenEmitItem(): Flow<ApolloResponse<Operation.Data>> {
        var firstCall = true
        val networkError = NETWORK_ERROR
        return flow {
            if (firstCall) {
                firstCall = false
                throw networkError
            } else {
                val response = mockk<ApolloResponse<Operation.Data>> {
                    mockkStatic("com.nabla.sdk.core.data.apollo.ApolloResponseExtKt")
                    every { dataOrThrowOnError } returns mockk()
                }
                emit(response)
            }
        }
    }

    companion object {
        private val NETWORK_ERROR = UnknownHostException()
    }
}
