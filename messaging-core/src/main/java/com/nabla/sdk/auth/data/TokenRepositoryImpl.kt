package com.nabla.sdk.auth.data

import com.nabla.sdk.auth.data.local.TokenLocalDataSource
import com.nabla.sdk.auth.data.remote.TokenRemoteDataSource
import com.nabla.sdk.auth.domain.boundary.SessionTokenProvider
import com.nabla.sdk.auth.domain.boundary.TokenRepository
import com.nabla.sdk.auth.domain.entity.AuthTokens
import com.nabla.sdk.messaging.core.kotlin.runCatchingCancellable

internal class TokenRepositoryImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val tokenRemoteDataSource: TokenRemoteDataSource,
    private val sessionTokenProvider: SessionTokenProvider
) : TokenRepository {

    override fun initSession(refreshToken: String, accessToken: String?) {
        tokenLocalDataSource.setRefreshToken(refreshToken)
        tokenLocalDataSource.setAccessToken(accessToken)
    }

    override suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean): Result<String> {
        val accessToken = tokenLocalDataSource.getAccessToken()
        if (accessToken != null && !accessToken.isExpired(1) && !forceRefreshAccessToken) {
            return Result.success(accessToken.toString())
        }
        // access token expired or not present, try refresh token
        val refreshToken = tokenLocalDataSource.getRefreshToken()
        if (refreshToken != null && !refreshToken.isExpired(1)) {
            runCatchingCancellable {
                val freshTokens = tokenRemoteDataSource.refresh(refreshToken.toString())
                tokenLocalDataSource.setAuthTokens(freshTokens)
                return Result.success(freshTokens.accessToken)
            }.onFailure {
                // Fallback to refreshing session
                return getNewSessionAuthTokens().map { it.accessToken }
            }
        }
        // no access or refresh tokens are valid, fallback to refreshing session
        return getNewSessionAuthTokens().map { it.accessToken }
    }

    private suspend fun getNewSessionAuthTokens(): Result<AuthTokens> {
        return sessionTokenProvider.fetchNewSessionAuthTokens().onSuccess {
            tokenLocalDataSource.setAuthTokens(it)
        }
    }
}