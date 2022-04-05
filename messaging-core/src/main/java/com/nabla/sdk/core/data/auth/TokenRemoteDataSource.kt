package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AuthTokens

internal class TokenRemoteDataSource(private val authService: AuthService) {

    suspend fun refresh(refreshToken: String): AuthTokens {
        return authService.refresh(refreshToken).let { response ->
            AuthTokens(response.refreshToken, response.accessToken)
        }
    }
}
