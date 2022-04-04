package com.nabla.sdk.core.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.coroutines.cancellation.CancellationException

inline fun <R> runCatchingCancellable(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}

fun <R> (suspend () -> R).asSharedSingleIn(
    coroutineScope: CoroutineScope
): SharedSingle<R> {
    return object : SharedSingle<R> {
        val sharedFlow = flow {
            val result = this@asSharedSingleIn.invoke()
            emit(result)
        }.shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
            replay = 1
        )

        override suspend fun await(): R {
            return sharedFlow.first()
        }
    }
}

interface SharedSingle<R> {
    suspend fun await(): R
}
