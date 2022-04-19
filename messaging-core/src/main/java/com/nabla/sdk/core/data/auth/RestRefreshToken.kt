package com.nabla.sdk.core.data.auth

import kotlinx.serialization.Serializable

@Serializable
internal class RestRefreshToken(
    val refresh_token: String
)
