package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.data.auth.AuthIoException
import com.nabla.sdk.core.domain.entity.NablaException

internal class BaseExceptionMapper : ExceptionMapper {
    override fun map(exception: Throwable): NablaException? {
        return when {
            exception is AuthIoException -> map(exception.cause)
            exception.isNetworkError() -> NablaException.Network(exception)
            exception is GraphQLException -> NablaException.Server(
                exception,
                exception.numericCode ?: 0,
                exception.serverMessage,
                exception.requestId,
            )
            else -> null
        }
    }
}
