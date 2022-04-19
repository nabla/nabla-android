package com.nabla.sdk.core

import com.nabla.sdk.messaging.core.BuildConfig

data class NablaCoreConfig(
    val baseUrl: String = "https://api.nabla.com/",
    val isLoggingEnabled: Boolean = BuildConfig.DEBUG,
    val additionalHeadersProvider: HeaderProvider? = null
)

data class Header(val name: String, val value: String)

interface HeaderProvider {
    fun headers(): List<Header>
}
