package com.nabla.sdk.core.kotlin

import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.coroutines.cancellation.CancellationException

@NablaInternal
public suspend inline fun <R> runCatchingCancellable(crossinline block: suspend () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}

@NablaInternal
public fun <R> sharedSingleIn(
    coroutineScope: CoroutineScope,
    block: suspend () -> R,
): SharedSingle<R> {
    return object : SharedSingle<R> {
        val sharedFlow = flow {
            val result = block()
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

@NablaInternal
public interface SharedSingle<R> {
    public suspend fun await(): R
}

internal fun <T> Flow<T>.shareInWithMaterializedErrors(
    scope: CoroutineScope,
    started: SharingStarted,
    replay: Int = 0
): Flow<T> {
    return map {
        Result.success(it)
    }.catch {
        emit(Result.failure(it))
    }.shareIn(
        scope, started, replay
    ).map {
        it.getOrThrow()
    }
}
