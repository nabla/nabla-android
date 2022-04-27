package com.nabla.sdk.core.data.exception

import com.apollographql.apollo3.exception.ApolloNetworkException
import okhttp3.internal.http2.ConnectionShutdownException
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

private const val IS_NETWORK_MAX_DEPTH = 2
