package com.nabla.sdk.core.data.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

internal interface NablaService {
    @POST("v1/jwt/user/refresh")
    suspend fun refresh(
        @Body refreshToken: String,
        @Tag authorizationType: AuthorizationType = AuthorizationType.NONE,
    ): RestSessionTokens
}
