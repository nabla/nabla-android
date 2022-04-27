package com.nabla.sdk.core.domain.boundary

interface Logger {
    fun debug(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun info(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun warn(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun error(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)

    companion object {
        private const val DEFAULT_TAG = "Nabla-SDK"
        internal const val AUTH_TAG = "$DEFAULT_TAG-Auth"
        fun asSdkTag(domain: String): String {
            return "$DEFAULT_TAG-${domain.replaceFirstChar { it.uppercaseChar() }}"
        }
    }
}
