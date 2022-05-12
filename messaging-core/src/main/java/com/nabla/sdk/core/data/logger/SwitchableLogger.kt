package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.domain.boundary.Logger

internal class SwitchableLogger(
    private val logger: Logger,
    private val isLoggingEnable: Boolean,
) : Logger {

    override fun debug(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        logger.debug(message, error, tag)
    }

    override fun info(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        logger.info(message, error, tag)
    }

    override fun warn(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        logger.warn(message, error, tag)
    }

    override fun error(message: String, error: Throwable?, tag: String) {
        if (!isLoggingEnable) return
        logger.error(message, error, tag)
    }
}
