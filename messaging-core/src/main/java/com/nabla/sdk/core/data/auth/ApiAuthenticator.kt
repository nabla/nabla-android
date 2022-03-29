package com.nabla.sdk.core.data.auth

import com.nabla.sdk.core.domain.boundary.TokenRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

internal class ApiAuthenticator(private val tokenRepository: TokenRepository): Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        return if (hasBearerToken(response)) {
            // Authenticated call, refresh if apply auth
            return reAuthenticateRequest(response)
        } else {
            // Unauthenticated call, no-op
            null
        }
    }

    private fun hasBearerToken(response: Response): Boolean {
        return response.request.header(HEADER_AUTH_NAME) != null
    }

    private fun reAuthenticateRequest(staleResponse: Response): Request? {
        if (responseCount(staleResponse) > AUTH_RETRY_COUNT) return null
        val freshAccessToken = runBlocking {
            tokenRepository.getFreshAccessToken(forceRefreshAccessToken = true).getOrNull()
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
