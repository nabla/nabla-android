package com.nabla.sdk.core.domain.boundary

public interface SessionClient {
    public fun initSession(sessionTokenProvider: SessionTokenProvider)
    public fun isSessionInitialized(): Boolean
    public suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): String
    public fun clearSession()
}
