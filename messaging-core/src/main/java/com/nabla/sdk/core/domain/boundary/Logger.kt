package com.nabla.sdk.core.domain.boundary

internal interface Logger {
    fun debug(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun info(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun warn(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun error(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)

    companion object {
        private const val DEFAULT_TAG = "Nabla-SDK"
        const val AUTH_TAG = "$DEFAULT_TAG-Auth"
        fun tag(domain: String): String = "$DEFAULT_TAG-$domain"
    }
}
