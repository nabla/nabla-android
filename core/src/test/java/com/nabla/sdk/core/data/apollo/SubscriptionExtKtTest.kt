package com.nabla.sdk.core.data.apollo

import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.nabla.sdk.tests.common.BaseCoroutineTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionExtKtTest : BaseCoroutineTest() {

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
