package com.nabla.sdk.core.reporting.error

import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger
import io.sentry.Breadcrumb
import io.sentry.Hub
import io.sentry.IHub
import io.sentry.NoOpHub
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message

internal class SentryErrorReporter(
    private val logger: Logger
) : ErrorReporter {

    private var hub: IHub = NoOpHub.getInstance()

    override fun enable(dsn: String, env: String) {
        val options = SentryOptions().apply {
            this.dsn = dsn
            this.environment = env
            isEnableUncaughtExceptionHandler = false
            isAttachServerName = false
            isAttachStacktrace = false
        }
        hub = Hub(options)
        options.integrations.forEach { it.register(hub, options) }
        logger.debug("Reporter enabled for env $env", domain = Logger.ERROR_REPORTING_DOMAIN)
    }

    override fun disable() {
        logger.debug("Reporter disabled", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub = NoOpHub.getInstance()
    }

    override fun reportException(throwable: Throwable) {
        logger.debug("Report throwable", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub.captureException(throwable)
    }

    override fun reportEvent(message: String) {
        logger.debug("Report event $message", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub.captureEvent(
            SentryEvent().apply {
                this.level = SentryLevel.INFO
                this.message = Message().apply {
                    this.message = message
                }
            },
        )
    }

    override fun log(message: String, metadata: Map<String, Any>?, domain: String?) {
        logger.debug("Log message $message", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub.addBreadcrumb(
            Breadcrumb().apply {
                level = SentryLevel.INFO
                this.message = message
                domain?.let { category = domain }
                metadata?.let { data.putAll(metadata) }
            }
        )
    }

    class Factory : ErrorReporter.Factory {
        override fun create(logger: Logger): ErrorReporter {
            return SentryErrorReporter(logger)
        }
    }
}

internal val Logger.Companion.ERROR_REPORTING_DOMAIN: String
    get() = "ErrorReporting"
