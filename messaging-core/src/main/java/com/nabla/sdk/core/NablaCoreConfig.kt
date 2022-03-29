package com.nabla.sdk.core

import com.nabla.sdk.messaging.core.BuildConfig

data class NablaCoreConfig(
    val baseUrl: String = "https://api.nabla.com/",
    val isLoggingEnable: Boolean = BuildConfig.DEBUG
)
