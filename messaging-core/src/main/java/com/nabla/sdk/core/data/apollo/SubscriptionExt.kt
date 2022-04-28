package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.kotlin.shareInWithMaterializedErrors
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.datetime.Clock
import kotlin.math.min

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

internal fun <D : Operation.Data> Flow<ApolloResponse<D>>.retryOnNetworkErrorAndShareIn(
    coroutineScope: CoroutineScope,
    action: (suspend (D) -> Unit)? = null
) = retryOnNetworkErrorWithExponentialBackoff()
    .map {
        it.dataAssertNoErrors
    }.onEach {
        if (action != null) { action(it) }
    }.shareInWithMaterializedErrors(
        scope = coroutineScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)
    )

internal fun <D> Flow<D>.notifyTypingUpdates(
    providersSelector: (D) -> List<ProviderInConversation>
): Flow<D> {
    return transformLatest { data ->
        emit(data)
        providersSelector(data).mapNotNull { provider ->
            provider.isInactiveAt()
        }.minOrNull()?.let {
            delay(it - Clock.System.now())
            emit(data)
        }
    }
}

private const val DELAY_UNIT = 5000L
private const val MAX_DELAY = DELAY_UNIT * 6
