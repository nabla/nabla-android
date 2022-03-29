package com.nabla.sdk.core.data.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class RestSessionTokens(
    val refreshToken: String,
    val accessToken: String
)
