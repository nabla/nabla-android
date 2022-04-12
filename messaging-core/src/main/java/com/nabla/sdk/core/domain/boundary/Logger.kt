package com.nabla.sdk.core.domain.boundary

private const val DEFAULT_TAG = "Nabla-SDK"

internal interface Logger {
    fun debug(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun info(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun warn(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
    fun error(message: String, error: Throwable? = null, tag: String = DEFAULT_TAG)
}
