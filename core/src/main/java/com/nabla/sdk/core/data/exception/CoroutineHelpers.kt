package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.annotation.NablaInternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@NablaInternal
public fun <T> Result<T>.mapFailureAsNablaException(exceptionMapper: NablaExceptionMapper): Result<T> {
    return mapFailure(exceptionMapper::map)
}

@NablaInternal
public fun <T> Result<T>.mapFailure(exceptionMapper: (Throwable) -> Throwable): Result<T> {
    return fold(
        onFailure = { Result.failure(exceptionMapper(it)) },
        onSuccess = { Result.success(it) }
    )
}

@NablaInternal
public fun <T> Flow<T>.catchAndRethrowAsNablaException(exceptionMapper: NablaExceptionMapper): Flow<T> = catch {
    throw exceptionMapper.map(it)
}
