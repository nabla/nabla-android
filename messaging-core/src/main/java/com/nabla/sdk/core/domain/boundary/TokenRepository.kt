package com.nabla.sdk.core.domain.boundary

internal interface TokenRepository {
    fun initSession(refreshToken: String, accessToken: String?)
    suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): String
    suspend fun clearSession()
}
