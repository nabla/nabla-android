package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public data class AuthTokens(val refreshToken: String, val accessToken: String) {
    @VisibleForTesting
    public companion object
}
