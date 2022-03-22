package com.nabla.sdk.auth.domain.data.auth

internal const val HEADER_AUTH_NAME = "AUTHORIZATION"
internal fun makeHeaderAuthValue(accessToken: String) = "Bearer $accessToken"