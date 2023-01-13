package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.data.exception.getRequestId
import com.nabla.sdk.core.data.exception.isNetworkError
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger

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

    private fun Throwable?.sendRequestIdIfAvailable() {
        if (this == null) {
            return
        }

        getRequestId()?.let { errorReporter.log(message = "RequestId: $it", domain = "GraphQL") }
    }

    private fun formatMessage(message: String, domain: String?): String {
        if (domain != null) {
            return "$domain - $message"
        }

        return message
    }
}
