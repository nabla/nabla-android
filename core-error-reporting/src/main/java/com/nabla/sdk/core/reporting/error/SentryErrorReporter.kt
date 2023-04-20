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
    private val logger: Logger,
) : ErrorReporter {
    /**
     * Map to save tags to reuse them if [hub] instance changes
     */
    private val tags = mutableMapOf<String, String>()

    /**
     * Map to save extras to reuse them if [hub] instance changes
     */
    private val extras = mutableMapOf<String, String>()

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

        tags.forEach { (name, value) ->
            hub.setTag(name, value)
        }

        extras.forEach { (name, value) ->
            hub.setExtra(name, value)
        }

        options.integrations.forEach { it.register(hub, options) }
        logger.debug("Reporter enabled for env $env", domain = Logger.ERROR_REPORTING_DOMAIN)
    }

    override fun disable() {
        logger.debug("Reporter disabled", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub = NoOpHub.getInstance()
    }

    override fun setTag(name: String, value: String) {
        hub.setTag(name, value)
        tags[name] = value
    }

    override fun setExtra(name: String, value: String) {
        hub.setExtra(name, value)
        extras[name] = value
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

    override fun reportWarning(message: String, throwable: Throwable?) {
        logger.debug("Report warning $message", domain = Logger.ERROR_REPORTING_DOMAIN)
        if (throwable != null) {
            hub.addBreadcrumb(message, "Warning")
            hub.captureEvent(
                SentryEvent(throwable).apply {
                    level = SentryLevel.WARNING
                },
            )
        } else {
            hub.captureMessage(message, SentryLevel.WARNING)
        }
    }

    override fun reportError(message: String, throwable: Throwable?) {
        logger.debug("Report error $message", domain = Logger.ERROR_REPORTING_DOMAIN)
        if (throwable != null) {
            hub.addBreadcrumb(message, "Error")
            hub.captureEvent(
                SentryEvent(throwable).apply {
                    level = SentryLevel.ERROR
                },
            )
        } else {
            hub.captureMessage(message, SentryLevel.ERROR)
        }
    }

    override fun log(message: String, metadata: Map<String, Any>?, domain: String?) {
        logger.debug("Log message $message", domain = Logger.ERROR_REPORTING_DOMAIN)
        hub.addBreadcrumb(
            Breadcrumb().apply {
                level = SentryLevel.INFO
                this.message = message
                domain?.let { category = domain }
                metadata?.let { data.putAll(metadata) }
            },
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
