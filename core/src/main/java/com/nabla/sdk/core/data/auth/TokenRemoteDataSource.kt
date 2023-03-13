package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AccessToken
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.RefreshToken

internal class TokenRemoteDataSource(private val authService: AuthService) {

    suspend fun refresh(refreshToken: String): AuthTokens {
        return authService.refresh(RestRefreshToken(refreshToken)).let { response ->
            AuthTokens(AccessToken(response.access_token), RefreshToken(response.refresh_token))
        }
    }
}
