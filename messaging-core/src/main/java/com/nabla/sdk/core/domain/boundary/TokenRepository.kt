package com.nabla.sdk.core.domain.boundary

internal interface TokenRepository {
    fun initSession(sessionTokenProvider: SessionTokenProvider)
    suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): String
    fun clearSession()
}
