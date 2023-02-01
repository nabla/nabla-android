package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.data.exception.WrappedOkhttpInterceptorException
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.AUTH_DOMAIN
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class AuthorizationInterceptor(
    private val logger: Logger,
    private val sessionClient: Lazy<SessionClient>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val updatedRequest: Request = when (AuthorizationType.fromRequest(chain.request())) {
            AuthorizationType.ACCESS_TOKEN -> {
                logger.debug(
                    domain = AUTH_DOMAIN,
                    message = "Using access token auth: ${chain.request()}",
                )
                val freshAccessToken = runBlocking {
                    runCatchingCancellable {
                        sessionClient.value.getFreshAccessToken()
                    }.getOrElse {
                        throw WrappedOkhttpInterceptorException(it)
                    }
                }

                chain.request().newBuilder()
                    .header(HEADER_AUTH_NAME, makeHeaderAuthValue(freshAccessToken))
                    .build()
            }
            AuthorizationType.NONE -> {
                logger.debug(
                    domain = AUTH_DOMAIN,
                    message = "Using no auth method: ${chain.request()}",
                )
                chain.request()
            }
        }

        val response = chain.proceed(updatedRequest)

        if (response.code == 401) {
            logger.warn(
                domain = AUTH_DOMAIN,
                message = "Current user received a 401 Unauthorized response from the server, invalidating tokens.",
            )

            sessionClient.value.markTokensAsInvalid()
        }

        return response
    }
}
