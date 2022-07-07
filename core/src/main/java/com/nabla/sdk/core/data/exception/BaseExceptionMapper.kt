package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.data.apollo.ApolloNoDataException
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.NetworkException
import com.nabla.sdk.core.domain.entity.ServerException
import retrofit2.HttpException

internal class BaseExceptionMapper : ExceptionMapper {
    override fun map(exception: Throwable): NablaException? {
        val unwrappedException: Throwable = exception.unwrapException()
        return mapOp(unwrappedException)
    }

    private fun mapOp(exception: Throwable): NablaException? {
        return when {
            exception is NablaException -> exception
            exception.isNetworkError() -> NetworkException(exception)
            exception is ApolloNoDataException -> ServerException(
                exception,
                code = -1,
                serverMessage = "The server did not return any data",
                requestId = exception.requestId,
            )
            exception is GraphQLException -> ServerException(
                exception,
                exception.numericCode ?: 0,
                exception.serverMessage,
                exception.requestId,
            )
            exception is HttpException -> {
                when (exception.code()) {
                    401 -> AuthenticationException.AuthorizationDenied(
                        cause = exception,
                    )
                    in 400..499 -> NetworkException(exception)
                    else -> ServerException(
                        exception,
                        code = exception.code(),
                        serverMessage = "HTTP error code ${exception.code()}",
                        requestId = "null",
                    )
                }
            }
            else -> null
        }
    }
}
