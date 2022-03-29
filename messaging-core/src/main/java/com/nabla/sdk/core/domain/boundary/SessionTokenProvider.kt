package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.AuthTokens

fun interface SessionTokenProvider {
    suspend fun fetchNewSessionAuthTokens(userId: String): Result<AuthTokens>
}
