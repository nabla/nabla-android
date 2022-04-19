package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.TokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class AuthorizationInterceptor(
    private val logger: Logger,
    private val tokenRepository: Lazy<TokenRepository>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val updatedRequest: Request = when (AuthorizationType.fromRequest(chain.request())) {
            AuthorizationType.ACCESS_TOKEN -> {
                logger.debug(
                    message = "Using access token auth: ${chain.request()}",
                    tag = Logger.AUTH_TAG
                )
                val freshAccessToken = runBlocking { tokenRepository.value.getFreshAccessToken().getOrThrow() }
                chain.request().newBuilder()
                    .header(HEADER_AUTH_NAME, makeHeaderAuthValue(freshAccessToken))
                    .build()
            }
            AuthorizationType.NONE -> {
                logger.debug(
                    message = "Using no auth method: ${chain.request()}",
                    tag = Logger.AUTH_TAG
                )
                chain.request()
            }
        }
        return chain.proceed(updatedRequest)
    }
}
