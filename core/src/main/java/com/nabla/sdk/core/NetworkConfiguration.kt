package com.nabla.sdk.core

/**
 * Nabla SDK Network configuration parameters.
 * This is exposed for internal usage and you should not have to use it in your app.
 *
 * @param baseUrl Optional — base url for Nabla API server.
 * @param additionalHeadersProvider Optional — useful to append additional query headers to http calls.
 */
public class NetworkConfiguration(
    internal val baseUrl: String = "https://api.nabla.com/",
    internal val additionalHeadersProvider: HeaderProvider? = null,
)
