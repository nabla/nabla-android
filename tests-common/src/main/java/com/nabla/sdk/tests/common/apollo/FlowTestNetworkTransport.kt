package com.nabla.sdk.tests.common.apollo

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A network transport used for test, that can be used to emit response from a hot flow. Typical
 * use case is controlling subscription emission during a test.
 */
class FlowTestNetworkTransport : NetworkTransport {

    private var isDisposed = false
    private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, Flow<ApolloResponse<out Operation.Data>>>()

    fun <D : Subscription.Data> register(subscription: Subscription<D>, dataProducer: Flow<D>) {
        registerOp(subscription, dataProducer)
    }

    fun <D : Query.Data> register(query: Query<D>, data: D) {
        registerOp(query, flowOf(data))
    }

    fun <D : Operation.Data> registerOp(operation: Operation<D>, dataProducer: Flow<D>) {
        operationsToResponses[operation] = dataProducer.map {
            ApolloResponse.Builder(
                operation = operation,
                requestUuid = uuid4(),
                data = it,
            ).isLast(true).build()
        }
    }

    override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
        if (isDisposed) {
            return emptyFlow()
        }

        val responsesFlow = operationsToResponses[request.operation] ?: return emptyFlow()

        @Suppress("UNCHECKED_CAST")
        return responsesFlow as Flow<ApolloResponse<D>>
    }

    override fun dispose() {
        isDisposed = true
    }
}
