package com.nabla.sdk.auth.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class RestSessionTokens(
    val refreshToken: String,
    val accessToken: String
)
