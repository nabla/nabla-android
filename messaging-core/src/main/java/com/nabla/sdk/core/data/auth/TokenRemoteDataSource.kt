package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AuthTokens

internal class TokenRemoteDataSource(private val authService: AuthService) {

    suspend fun refresh(refreshToken: String): AuthTokens {
        return authService.refresh(RestRefreshToken(refreshToken)).let { response ->
            AuthTokens(response.refresh_token, response.access_token)
        }
    }
}
