package com.nabla.sdk.core.kotlin

import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.coroutines.cancellation.CancellationException

@NablaInternal
public interface SharedSingle<out R, out T : Result<R>> {
    public suspend fun await(): R
}

@NablaInternal
public object KotlinExt {
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
    ): SharedSingle<R, Result<R>> {
        return object : SharedSingle<R, Result<R>> {
            val sharedFlow = flow {
                val result = runCatchingCancellable { block() }
                emit(result)
            }.shareIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                replay = 1,
            )

            override suspend fun await(): R {
                return sharedFlow.first().getOrThrow()
            }
        }
    }

    internal fun <T> Flow<T>.shareInWithMaterializedErrors(
        scope: CoroutineScope,
        started: SharingStarted,
        replay: Int = 0,
    ): Flow<T> {
        return map {
            Result.success(it)
        }.catch {
            emit(Result.failure(it))
        }.shareIn(
            scope,
            started,
            replay,
        ).map {
            it.getOrThrow()
        }
    }

    @NablaInternal
    public fun <T1, T2, T3, T4, T5, T6, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
    ): Flow<R> {
        val tripleFlow1 = combine(flow, flow2, flow3, ::Triple)
        val tripleFlow2 = combine(flow4, flow5, flow6, ::Triple)
        return combine(tripleFlow1, tripleFlow2) { triple1, triple2 ->
            transform(
                triple1.first,
                triple1.second,
                triple1.third,
                triple2.first,
                triple2.second,
                triple2.third,
            )
        }
    }
}
