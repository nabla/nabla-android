package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.exception.ApolloNetworkException
import com.nabla.sdk.core.domain.entity.NablaException
import okhttp3.internal.http2.ConnectionShutdownException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal class BaseExceptionMapper : ExceptionMapper {
    override fun map(exception: Throwable): NablaException? {
        return when {
            exception.isNetworkError() -> NablaException.Network(exception)
            exception is GraphQLException -> when (val errorCode = exception.errorCode) {
                ErrorCode.BAD_REQUEST,
                ErrorCode.INTERNAL_SERVER_ERROR -> NablaException.Server(exception, errorCode.code)
                else -> NablaException.Internal(exception, errorCode?.code ?: 0)
            }
            else -> null
        }
    }

    private fun Throwable.isNetworkError(depth: Int = 0): Boolean =
        this is InterruptedIOException ||
            this is UnknownHostException ||
            this is SocketException ||
            this is SSLException ||
            this is ConnectionShutdownException ||
            this is ApolloNetworkException ||
            (depth <= IS_NETWORK_MAX_DEPTH && cause?.isNetworkError(depth + 1) ?: false)

    companion object {
        private const val IS_NETWORK_MAX_DEPTH = 2
    }
}
