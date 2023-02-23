package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.AuthTokens

/**
 * Callback from Nabla SDK to request new server-made access and refresh tokens.
 *
 * You will typically call your server here and make sure no caching is used:
 * the SDK is only interested in fresh versions each time it calls back.
 */
public fun interface SessionTokenProvider {
    public suspend fun fetchNewSessionAuthTokens(userId: String): Result<AuthTokens>
}
