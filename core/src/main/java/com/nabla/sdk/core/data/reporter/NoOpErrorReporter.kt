package com.nabla.sdk.core.data.reporter

import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger

internal class NoOpErrorReporter : ErrorReporter {

    override fun enable(dsn: String, env: String) {
        /* no-op */
    }

    override fun disable() {
        /* no-op */
    }

    override fun setTag(name: String, value: String) {
        /* no-op */
    }

    override fun setExtra(name: String, value: String) {
        /* no-op */
    }

    override fun reportEvent(message: String) {
        /* no-op */
    }

    override fun reportWarning(message: String, throwable: Throwable?) {
        /* no-op */
    }

    override fun reportError(message: String, throwable: Throwable?) {
        /* no-op */
    }

    override fun log(message: String, metadata: Map<String, Any>?, domain: String?) {
        /* no-op */
    }

    class Factory : ErrorReporter.Factory {
        override fun create(logger: Logger): ErrorReporter = NoOpErrorReporter()
    }
}
