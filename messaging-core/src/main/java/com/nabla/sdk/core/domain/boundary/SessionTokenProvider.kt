package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.StringId

fun interface SessionTokenProvider {
    suspend fun fetchNewSessionAuthTokens(userId: StringId): Result<AuthTokens>
}
