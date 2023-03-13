package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AccessToken
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.RefreshToken

internal class TokenLocalDataSource {
    private var refreshToken: RefreshToken? = null
    private var accessToken: AccessToken? = null

    fun setAuthTokens(authTokens: AuthTokens) {
        this.refreshToken = authTokens.refreshToken
        this.accessToken = authTokens.accessToken
    }

    fun getAuthTokens(): AuthTokens? {
        val currentRefreshToken = refreshToken
        val currentAccessToken = accessToken
        return if (currentRefreshToken != null && currentAccessToken != null) {
            AuthTokens(currentAccessToken, currentRefreshToken)
        } else {
            null
        }
    }

    fun clear() {
        refreshToken = null
        accessToken = null
    }
}
