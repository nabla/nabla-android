package com.nabla.sdk.core.data.auth

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

internal interface AuthService {
    @POST("v1/patient/jwt/refresh")
    suspend fun refresh(
        @Body refreshToken: RestRefreshToken,
        @Tag authorizationType: AuthorizationType = AuthorizationType.NONE,
    ): RestSessionTokens
}
