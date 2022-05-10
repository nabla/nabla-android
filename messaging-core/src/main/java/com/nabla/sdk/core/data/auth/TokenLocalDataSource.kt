package com.nabla.sdk.core.data.auth

import com.auth0.android.jwt.JWT
import com.nabla.sdk.core.domain.entity.AuthTokens

internal class TokenLocalDataSource {
    private var refreshToken: String? = null
    private var accessToken: String? = null

    fun setRefreshToken(refreshToken: String?) {
        this.refreshToken = refreshToken
    }

    fun getRefreshToken(): JWT? = refreshToken?.let { JWT(it) }

    fun setAccessToken(accessToken: String?) {
        this.accessToken = accessToken
    }

    fun getAccessToken(): JWT? = accessToken?.let { JWT(it) }

    fun setAuthTokens(authTokens: AuthTokens) {
        setRefreshToken(authTokens.refreshToken)
        setAccessToken(authTokens.accessToken)
    }

    fun clear() {
        refreshToken = null
        accessToken = null
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "refresh-token"
        private const val KEY_ACCESS_TOKEN = "access-token"
    }
}
