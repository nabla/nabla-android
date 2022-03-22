package com.nabla.sdk.auth.data.remote

import com.nabla.sdk.auth.domain.entity.AuthTokens

internal class TokenRemoteDataSource(private val nablaService: NablaService) {

    suspend fun refresh(refreshToken: String): AuthTokens {
        return nablaService.refresh(refreshToken).let { response ->
            AuthTokens(response.refreshToken, response.accessToken)
        }
    }

}
