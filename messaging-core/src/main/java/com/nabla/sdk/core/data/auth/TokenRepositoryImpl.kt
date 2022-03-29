package com.nabla.sdk.core.data.auth

import com.auth0.android.jwt.JWT
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.boundary.TokenRepository
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.messaging.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class TokenRepositoryImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val tokenRemoteDataSource: TokenRemoteDataSource,
    private val sessionTokenProvider: SessionTokenProvider,
    private val patientRepository: PatientRepository,
    private val logger: Logger,
) : TokenRepository {

    private val refreshLock = Mutex()

    override fun initSession(refreshToken: String, accessToken: String?) {
        tokenLocalDataSource.setRefreshToken(refreshToken)
        tokenLocalDataSource.setAccessToken(accessToken)
    }

    override suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean): Result<String> {
        return refreshLock.withLock {
            getFreshAccessTokenUnsafe(forceRefreshAccessToken)
        }
    }

    private suspend fun getFreshAccessTokenUnsafe(
        forceRefreshAccessToken: Boolean
    ): Result<String> {
        logger.debug("get fresh access token...")
        val accessToken = tokenLocalDataSource.getAccessToken()
        if (accessToken.isValid() && !forceRefreshAccessToken) {
            return Result.success(accessToken.toString())
        }
        // access token expired or not present, try refresh token
        val refreshToken = tokenLocalDataSource.getRefreshToken()
        if (refreshToken.isValid()) {
            runCatchingCancellable {
                val freshTokens = tokenRemoteDataSource.refresh(refreshToken.toString())
                tokenLocalDataSource.setAuthTokens(freshTokens)
                return Result.success(freshTokens.accessToken)
            }.onFailure { refreshTokenError ->
                // Fallback to refreshing session
                return renewSessionAuthTokens().map {
                    it.accessToken
                }.onFailure { renewSessionError ->
                    // Ensure no exception is suppressed
                    return Result.failure(renewSessionError.apply { addSuppressed(refreshTokenError) })
                }
            }
        }
        // no access or refresh tokens are valid, fallback to refreshing session
        return renewSessionAuthTokens().map { it.accessToken }
    }

    override suspend fun clearSession() {
        refreshLock.withLock {
            tokenLocalDataSource.clear()
        }
    }

    private suspend fun renewSessionAuthTokens(): Result<AuthTokens> {
        val patientId = patientRepository.getPatientId()
            ?: return Result.failure(IllegalStateException("No patient set"))
        return sessionTokenProvider.fetchNewSessionAuthTokens(patientId).onSuccess {
            tokenLocalDataSource.setAuthTokens(it)
        }
    }
}

private fun JWT?.isValid(): Boolean {
    return this != null && !this.isExpired(1)
}
