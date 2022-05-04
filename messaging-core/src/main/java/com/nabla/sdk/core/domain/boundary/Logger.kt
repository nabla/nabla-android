package com.nabla.sdk.core.domain.boundary

public interface Logger {
    public fun debug(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    public fun info(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    public fun warn(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    public fun error(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)

    public companion object {
        private const val DEFAULT_TAG = "Nabla-SDK"
        internal const val AUTH_TAG = "$DEFAULT_TAG-Auth"
        public fun asSdkTag(domain: String): String {
            return "$DEFAULT_TAG-${domain.replaceFirstChar { it.uppercaseChar() }}"
        }
    }
}
