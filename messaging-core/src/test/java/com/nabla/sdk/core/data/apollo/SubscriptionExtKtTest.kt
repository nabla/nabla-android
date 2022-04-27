package com.nabla.sdk.core.data.apollo

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.UnknownHostException

class SubscriptionExtKtTest {
    @Test
    fun `gql subscription recovers from network error`() {
        val gqlFlow = gqlFlowThatThrowsNetworkErrorThenEmitItem()
        assertGqlFlowBehaviour(gqlFlow) {
            awaitItem()
        }
    }

    private fun gqlFlowThatThrowsNetworkErrorThenEmitItem(): Flow<ApolloResponse<Operation.Data>> {
        var firstCall = true
        val networkError = UnknownHostException()
        return flow {
            if (firstCall) {
                firstCall = false
                throw networkError
            } else {
                val response = mockk<ApolloResponse<Operation.Data>> {
                    every { dataAssertNoErrors } returns mockk()
                }
                emit(response)
            }
        }
    }

    private fun assertGqlFlowBehaviour(
        gqlFlow: Flow<ApolloResponse<Operation.Data>>,
        validate: suspend FlowTurbine<Operation.Data>.() -> Unit
    ) = runTest {
        val job = Job()
        gqlFlow.retryOnNetworkErrorAndShareIn(this + job).test(validate = validate)
        // Cancelling hot flow as it never completes itself, which would prevent runTest to complete
        job.cancel()
    }
}
