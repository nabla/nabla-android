package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.data.apollo.ApolloNoDataException
import com.nabla.sdk.core.domain.entity.NablaException

internal class BaseExceptionMapper : ExceptionMapper {
    override fun map(exception: Throwable): NablaException? {
        val unwrappedException: Throwable = exception.unwrapException()
        return mapOp(unwrappedException)
    }

    private fun mapOp(exception: Throwable): NablaException? {
        return when {
            exception is NablaException -> exception
            exception.isNetworkError() -> NablaException.Network(exception)
            exception is ApolloNoDataException -> NablaException.Server(
                exception,
                code = -1,
                serverMessage = "The server did not return any data",
                requestId = exception.requestId,
            )
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
