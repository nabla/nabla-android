package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.entity.AuthTokens

internal class TokenRemoteDataSource(private val nablaService: NablaService) {

    suspend fun refresh(refreshToken: String): AuthTokens {
        return nablaService.refresh(refreshToken).let { response ->
            AuthTokens(response.refreshToken, response.accessToken)
        }
    }

}
