package com.nabla.sdk.core.domain.entity

import android.util.Log
import com.nabla.sdk.core.domain.boundary.Logger

/**
 * An implementation of [Logger] that logs to the Android logcat.
 *
 * @param logLevel the minimum level of messages to log, defaults to [LogLevel.WARN]
 */
public class LogcatLogger(
    private val logLevel: LogLevel = LogLevel.WARN,
) : Logger {
    override fun debug(message: String, error: Throwable?, domain: String) {
        if (logLevel.isAtLeast(LogLevel.DEBUG)) {
            Log.d(asSdkTag(domain), message, error)
        }
    }

    override fun info(message: String, error: Throwable?, domain: String?) {
        if (logLevel.isAtLeast(LogLevel.INFO)) {
            Log.i(asSdkTag(domain), message, error)
        }
    }

    override fun warn(message: String, error: Throwable?, domain: String?) {
        if (logLevel.isAtLeast(LogLevel.WARN)) {
            Log.w(asSdkTag(domain), message, error)
        }
    }

    override fun error(message: String, error: Throwable?, domain: String?) {
        if (logLevel.isAtLeast(LogLevel.ERROR)) {
            Log.e(asSdkTag(domain), message, error)
        }
    }

    private fun LogLevel.isAtLeast(level: LogLevel) = value <= level.value

    public enum class LogLevel(internal val value: Int) {
        /**
         * Get access to debug logs including network traces and events. Not suitable for production
         * use or normal development process.
         */
        DEBUG(0),

        /**
         * Logs about things happening that are not necessarily important, but that may
         * give you some insight into the behavior of the SDK.
         */
        INFO(1),

        /**
         * Something went wrong due to an external condition (like a network issue) and this error
         * will be handled by the SDK (returned as [Result] or handled by the UI component).
         */
        WARN(2),

        /**
         * An unexpected error occurred and the SDK will not handle it. You might try to update
         * the SDK to a newer version or contact the support team if that happens.
         */
        ERROR(3),
    }

    private companion object {
        private const val DEFAULT_TAG = "Nabla-SDK"

        private fun asSdkTag(domain: String?): String {
            if (domain == null) {
                return DEFAULT_TAG
            }

            return "$DEFAULT_TAG-${domain.replaceFirstChar { it.uppercaseChar() }}"
        }
    }
}
