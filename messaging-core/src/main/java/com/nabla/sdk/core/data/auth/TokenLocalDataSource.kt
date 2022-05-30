package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AuthTokens

internal class TokenLocalDataSource {
    private var refreshToken: String? = null
    private var accessToken: String? = null

    fun setAuthTokens(authTokens: AuthTokens) {
        this.refreshToken = authTokens.refreshToken
        this.accessToken = authTokens.accessToken
    }

    fun getAuthTokens(): AuthTokens? {
        val currentRefreshToken = refreshToken
        val currentAccessToken = accessToken
        return if (currentRefreshToken != null && currentAccessToken != null) {
            AuthTokens(currentRefreshToken, currentAccessToken)
        } else {
            null
        }
    }

    fun clear() {
        refreshToken = null
        accessToken = null
    }
}
