package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Subscription
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.kotlin.KotlinExt.shareInWithMaterializedErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.min

@NablaInternal
public object SubscriptionExt {
    internal fun <D : Operation.Data> Flow<ApolloResponse<D>>.retryOnNetworkErrorWithExponentialBackoff(): Flow<ApolloResponse<D>> {
        return retryWhen { cause, attempt ->
            if (cause.isNetworkError()) {
                val delayMs = min(MAX_DELAY, attempt * DELAY_UNIT)
                delay(delayMs)
                true
            } else {
                false
            }
        }
    }

    @NablaInternal
    public fun <D : Operation.Data> Flow<ApolloResponse<D>>.retryOnNetworkErrorAndShareIn(
        coroutineScope: CoroutineScope,
    ): Flow<ApolloResponse<D>> = retryOnNetworkErrorWithExponentialBackoff()
        .shareInWithMaterializedErrors(
            scope = coroutineScope,
            replay = 0,
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    @NablaInternal
    public fun <D : Subscription.Data> ApolloCall<D>.toFlowAsRetryable(): Flow<ApolloResponse<D>> =
        flowOf(this)
            .flatMapLatest { it.toFlow() }
}

private const val DELAY_UNIT = 5000L
private const val MAX_DELAY = DELAY_UNIT * 6
