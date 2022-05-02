package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.StringId

/**
 * Callback from Nabla SDK to request new server-made access and refresh tokens.
 *
 * You will typically call your server here and make sure no caching is used:
 * the SDK is only interested in fresh versions each time it calls back.
 */
fun interface SessionTokenProvider {
    suspend fun fetchNewSessionAuthTokens(userId: StringId): Result<AuthTokens>
}
