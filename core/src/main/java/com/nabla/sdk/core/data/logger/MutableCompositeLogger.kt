package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.domain.boundary.Logger

internal class MutableCompositeLogger constructor() : Logger {
    private val loggers = mutableListOf<Logger>()

    constructor(logger: Logger) : this() {
        loggers.add(logger)
    }

    fun addLogger(logger: Logger) {
        loggers.add(logger)
    }

    override fun debug(message: String, error: Throwable?, domain: String) {
        for (logger in loggers) {
            logger.debug(message, error, domain)
        }
    }

    override fun info(message: String, error: Throwable?, domain: String?) {
        for (logger in loggers) {
            logger.info(message, error, domain)
        }
    }

    override fun warn(message: String, error: Throwable?, domain: String?) {
        for (logger in loggers) {
            logger.warn(message, error, domain)
        }
    }

    override fun error(message: String, error: Throwable?, domain: String?) {
        for (logger in loggers) {
            logger.error(message, error, domain)
        }
    }
}
