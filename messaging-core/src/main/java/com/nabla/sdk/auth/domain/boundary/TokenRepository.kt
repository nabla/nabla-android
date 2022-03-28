package com.nabla.sdk.auth.domain.boundary

interface TokenRepository {
    fun initSession(refreshToken: String, accessToken: String?)
    suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): Result<String>
    suspend fun clearSession()
}
