package com.nabla.sdk.core

import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.messaging.core.BuildConfig

data class NablaCoreConfig(
    val publicApiKey: String,
    val baseUrl: String = "https://api.nabla.com/",
    val isLoggingEnabled: Boolean = BuildConfig.DEBUG,
    val sessionTokenProvider: SessionTokenProvider,
    val additionalHeadersProvider: HeaderProvider? = null,
)

data class Header(val name: String, val value: String)

interface HeaderProvider {
    fun headers(): List<Header>
}
