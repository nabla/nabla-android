package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.Id

fun interface SessionTokenProvider {
    suspend fun fetchNewSessionAuthTokens(userId: Id): Result<AuthTokens>
}
