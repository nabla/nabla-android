package com.nabla.sdk.auth.domain.boundary

import com.nabla.sdk.auth.domain.entity.AuthTokens

fun interface SessionTokenProvider {
    suspend fun fetchNewSessionAuthTokens(): Result<AuthTokens>
}
