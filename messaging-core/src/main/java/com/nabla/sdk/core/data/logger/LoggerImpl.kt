package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.domain.boundary.Logger

internal class LoggerImpl(
    private val androidLogger: AndroidLogger,
    private val isLoggingEnable: Boolean,
) : Logger {

    override fun debug(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        androidLogger.debug(message, error, tag)
    }

    override fun info(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        androidLogger.info(message, error, tag)
    }

    override fun warn(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        androidLogger.warn(message, error, tag)
    }

    override fun error(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        androidLogger.error(message, error, tag)
    }
}
