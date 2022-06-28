package com.nabla.sdk.core.data.auth

internal const val HEADER_AUTH_NAME = "X-Nabla-Authorization"
internal fun makeHeaderAuthValue(accessToken: String) = "Bearer $accessToken"
