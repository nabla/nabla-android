package com.nabla.sdk.core.data.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal fun <T> Result<T>.mapFailureAsNablaException(exceptionMapper: NablaExceptionMapper): Result<T> {
    return mapFailure(exceptionMapper::map)
}

internal fun <T> Result<T>.mapFailure(exceptionMapper: (Throwable) -> Throwable): Result<T> {
    return fold(
        onFailure = { Result.failure<T>(exceptionMapper(it)) },
        onSuccess = { Result.success<T>(it) }
    )
}

internal fun <T> Flow<T>.catchAndRethrowAsNablaException(exceptionMapper: NablaExceptionMapper) = catch {
    throw exceptionMapper.map(it)
}
