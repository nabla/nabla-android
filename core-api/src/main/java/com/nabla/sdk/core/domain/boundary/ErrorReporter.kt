package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public interface ErrorReporter {

    @NablaInternal
    public fun enable(dsn: String, env: String)

    @NablaInternal
    public fun disable()

    @NablaInternal
    public fun reportException(throwable: Throwable)

    @NablaInternal
    public fun reportEvent(message: String)

    @NablaInternal
    public fun log(message: String, metadata: Map<String, Any>? = null, domain: String? = null)

    @NablaInternal
    public interface Factory {

        @NablaInternal
        public fun create(logger: Logger): ErrorReporter
    }

    public companion object {
        @NablaInternal
        public var reporterFactory: Factory? = null
    }
}
