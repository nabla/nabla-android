package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public interface SessionClient {
    public fun initSession(sessionTokenProvider: SessionTokenProvider)
    public fun isSessionInitialized(): Boolean
    public suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean = false): String
    public fun markTokensAsInvalid()
    public fun clearSession()
}
