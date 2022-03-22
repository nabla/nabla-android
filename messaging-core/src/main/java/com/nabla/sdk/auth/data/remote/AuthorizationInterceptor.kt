package com.nabla.sdk.auth.data.remote

import com.nabla.sdk.auth.domain.boundary.TokenRepository
import com.nabla.sdk.auth.domain.data.auth.HEADER_AUTH_NAME
import com.nabla.sdk.auth.domain.data.auth.makeHeaderAuthValue
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class AuthorizationInterceptor(private val tokenRepository: TokenRepository) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val updatedRequest: Request = when (AuthorizationType.fromRequest(chain.request())) {
            AuthorizationType.ACCESS_TOKEN -> {
                val freshAccessToken = runBlocking { tokenRepository.getFreshAccessToken().getOrThrow() }
                chain.request().newBuilder()
                    .header(HEADER_AUTH_NAME, makeHeaderAuthValue(freshAccessToken))
                    .build()
            }
            AuthorizationType.NONE -> chain.request()
        }
        return chain.proceed(updatedRequest)
    }
}
