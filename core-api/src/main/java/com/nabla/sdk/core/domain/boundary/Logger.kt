package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal

/**
 * Abstraction of the logging layer of the SDK. You can implement this interface yourself to get
 * access to SDK logs and errors in your application or you can use the default implementation
 * provided by the SDK that just outputs logs in the logcat.
 */
public interface Logger {
    public fun debug(message: String, error: Throwable? = null, domain: String)
    public fun info(message: String, error: Throwable? = null, domain: String? = null)
    public fun warn(message: String, error: Throwable? = null, domain: String? = null)
    public fun error(message: String, error: Throwable? = null, domain: String? = null)

    @NablaInternal
    public companion object {
        public const val AUTH_DOMAIN: String = "Auth"
        public const val HTTP_DOMAIN: String = "Http"
        public const val GQL_DOMAIN: String = "Gql"
    }
}
