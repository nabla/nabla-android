package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.boundary.Logger
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class ApiAuthenticator(
    private val logger: Logger,
    private val tokenRepository: Lazy<TokenRepositoryImpl>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        logger.debug(
            message = "Request ${response.request} requires authentication",
            tag = Logger.AUTH_TAG
        )
        return if (hasBearerToken(response)) {
            // Authenticated call, refresh if apply auth
            return reAuthenticateRequest(response)
        } else {
            // Unauthenticated call, no-op
            logger.debug(
                message = "Request ${response.request} is not using any auth method, check API or decorate call with AuthorizationType",
                tag = Logger.AUTH_TAG
            )
            null
        }
    }

    private fun hasBearerToken(response: Response): Boolean {
        return response.request.header(HEADER_AUTH_NAME) != null
    }

    private fun reAuthenticateRequest(staleResponse: Response): Request? {
        if (responseCount(staleResponse) > AUTH_RETRY_COUNT) {
            return null
        }
        val freshAccessToken = runBlocking {
            tokenRepository.value.getFreshAccessToken(forceRefreshAccessToken = true).getOrNull()
        } ?: return null
        return staleResponse.request.newBuilder()
            .header(HEADER_AUTH_NAME, makeHeaderAuthValue(freshAccessToken))
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var currentResponse: Response? = response
        while (currentResponse?.priorResponse != null) {
            currentResponse = currentResponse.priorResponse
            result++
        }
        return result
    }

    companion object {
        private const val AUTH_RETRY_COUNT = 3
    }
}
