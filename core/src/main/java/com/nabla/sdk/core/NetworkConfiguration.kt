package com.nabla.sdk.core

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException

/**
 * Nabla SDK Network configuration parameters.
 * This is exposed for internal usage and you should not have to use it in your app.
 *
 * @param baseUrl Optional — base url for Nabla API server.
 * @param additionalHeadersProvider Optional — useful to append additional query headers to http calls.
 */
@NablaInternal
public data class NetworkConfiguration(
    internal val baseUrl: String = "https://api.nabla.com/",
    internal val additionalHeadersProvider: HeaderProvider? = null,
) {
    init {
        if (baseUrl.last() != '/') {
            throwNablaInternalException("baseUrl($baseUrl) must end with a /")
        }
    }
}

@NablaInternal
public data class Header(val name: String, val value: String)

@NablaInternal
public interface HeaderProvider {
    public fun headers(): List<Header>
}
