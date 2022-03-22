package com.nabla.sdk.auth.data.local

import com.auth0.android.jwt.JWT
import com.nabla.sdk.auth.domain.entity.AuthTokens
import com.nabla.sdk.core.data.SecuredKVStorage

internal class TokenLocalDataSource(private val securedKVStorage: SecuredKVStorage) {

    fun setRefreshToken(refreshToken: String?) {
        with (securedKVStorage.edit()) {
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun getRefreshToken(): JWT? {
        return securedKVStorage.getString(KEY_REFRESH_TOKEN, null)?.let { JWT(it) }
    }

    fun setAccessToken(accessToken: String?) {
        with (securedKVStorage.edit()) {
            putString(KEY_ACCESS_TOKEN, accessToken)
            apply()
        }
    }

    fun getAccessToken(): JWT? {
        return securedKVStorage.getString(KEY_ACCESS_TOKEN, null)?.let { JWT(it) }
    }

    fun setAuthTokens(authTokens: AuthTokens) {
        with (securedKVStorage.edit()) {
            putString(KEY_REFRESH_TOKEN, authTokens.refreshToken)
            putString(KEY_ACCESS_TOKEN, authTokens.accessToken)
            apply()
        }
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "refresh-token"
        private const val KEY_ACCESS_TOKEN = "access-token"
    }
}