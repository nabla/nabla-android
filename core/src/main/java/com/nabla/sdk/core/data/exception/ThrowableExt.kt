package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.get
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.nabla.sdk.core.data.apollo.REQUEST_ID_HEADER_NAME
import okhttp3.internal.http2.ConnectionShutdownException
import retrofit2.HttpException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal fun Throwable.isNetworkError(depth: Int = 0): Boolean =
    this is InterruptedIOException ||
        this is UnknownHostException ||
        this is SocketException ||
        this is SSLException ||
        this is ConnectionShutdownException ||
        this is ApolloNetworkException ||
        (depth <= IS_NETWORK_MAX_DEPTH && cause?.isNetworkError(depth + 1) ?: false)

@OptIn(ApolloExperimental::class)
internal fun Throwable.getRequestId(depth: Int = 0): String? {
    val requestId = when (this) {
        is GraphQLException -> requestId
        is ApolloHttpException -> this.headers.get(REQUEST_ID_HEADER_NAME)
        is HttpException -> response()?.headers()?.get(REQUEST_ID_HEADER_NAME)
        else -> null
    }

    if (requestId != null) {
        return requestId
    }

    return if (depth <= REQUEST_ID_MAX_DEPTH) { cause?.getRequestId(depth + 1) } else { null }
}

internal fun Throwable.unwrapException(): Throwable {
    return when (this) {
        is WrappedOkhttpInterceptorException -> cause
        else -> this
    }
}

private const val IS_NETWORK_MAX_DEPTH = 2
private const val REQUEST_ID_MAX_DEPTH = 2
