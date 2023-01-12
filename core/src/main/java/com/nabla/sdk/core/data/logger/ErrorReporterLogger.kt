package com.nabla.sdk.core.data.logger

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.get
import com.apollographql.apollo3.exception.ApolloHttpException
import com.nabla.sdk.core.data.apollo.REQUEST_ID_HEADER_NAME
import com.nabla.sdk.core.data.exception.GraphQLException
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger
import retrofit2.HttpException

internal class ErrorReporterLogger(
    private val errorReporter: ErrorReporter,
    publicApiKey: String,
    sdkVersion: String,
    phoneName: String,
    androidApiLevel: Int,
) : Logger {

    init {
        errorReporter.setExtra("ApiKey", publicApiKey) // Tags are limited to 200 chars to we use an extra for the API key
        errorReporter.setTag("SdkVersion", sdkVersion)
        errorReporter.setTag("PhoneName", phoneName)
        errorReporter.setTag("AndroidApiLevel", androidApiLevel.toString())
    }

    override fun debug(message: String, error: Throwable?, domain: String) {
        // No-op
    }

    override fun info(message: String, error: Throwable?, domain: String?) {
        // No-op
    }

    override fun warn(message: String, error: Throwable?, domain: String?) {
        if (error?.isNetworkError() == true) {
            return
        }

        error.sendRequestIdIfAvailable()
        errorReporter.reportWarning(formatMessage(message, domain), error)
    }

    override fun error(message: String, error: Throwable?, domain: String?) {
        if (error?.isNetworkError() == true) {
            return
        }

        error.sendRequestIdIfAvailable()
        errorReporter.reportError(formatMessage(message, domain), error)
    }

    @OptIn(ApolloExperimental::class)
    private fun Throwable?.sendRequestIdIfAvailable() {
        if (this == null) {
            return
        }

        val requestId = when (this) {
            is GraphQLException -> requestId
            is ApolloHttpException -> this.headers.get(REQUEST_ID_HEADER_NAME)
            is HttpException -> response()?.headers()?.get(REQUEST_ID_HEADER_NAME)
            else -> null
        }

        requestId?.let { errorReporter.log(message = "RequestId: $requestId", domain = "GraphQL") }
    }

    private fun formatMessage(message: String, domain: String?): String {
        if (domain != null) {
            return "$domain - $message"
        }

        return message
    }
}
