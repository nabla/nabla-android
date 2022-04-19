package com.nabla.sdk.core.data.exception

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

internal fun <T> Result<T>.mapFailureAsNablaException(exceptionMapper: NablaExceptionMapper): Result<T> {
    return when (val exception = exceptionOrNull()) {
        null -> this
        else -> Result.failure(exceptionMapper.map(exception))
    }
}

internal fun <T> Flow<T>.catchAndRethrowAsNablaException(exceptionMapper: NablaExceptionMapper) = catch {
    throw exceptionMapper.map(it)
}
