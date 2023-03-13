package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public data class AuthTokens(val accessToken: AccessToken, val refreshToken: RefreshToken) {
    @VisibleForTesting
    public companion object
}

@JvmInline
public value class AccessToken(public val token: String)

@JvmInline
public value class RefreshToken(public val token: String)
