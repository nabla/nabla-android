package com.nabla.sdk.core.data.auth

import kotlinx.serialization.Serializable

@Serializable
internal data class RestSessionTokens(
    val refresh_token: String,
    val access_token: String,
)
