package com.nabla.sdk.core.data.auth

import com.auth0.android.jwt.JWT
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SessionClientImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val tokenRemoteDataSource: TokenRemoteDataSource,
    private val patientRepository: PatientRepository,
    private val logger: Logger,
    private val exceptionMapper: NablaExceptionMapper,
) : SessionClient {

    private val refreshLock = Mutex()

    private var sessionTokenProvider: SessionTokenProvider? = null

    override fun initSession(sessionTokenProvider: SessionTokenProvider) {
        this.sessionTokenProvider = sessionTokenProvider
    }

    override fun isSessionInitialized(): Boolean = sessionTokenProvider != null

    override suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean): String {
        return refreshLock.withLock {
            getFreshAccessTokenUnsafe(forceRefreshAccessToken)
        }
    }

    private suspend fun getFreshAccessTokenUnsafe(
        forceRefreshAccessToken: Boolean
    ): String {
        logger.debug(
            message = "get fresh access token with forceRefreshAccessToken=$forceRefreshAccessToken...",
            tag = Logger.AUTH_TAG
        )
        val authTokens = tokenLocalDataSource.getAuthTokens() ?: renewSessionAuthTokens()
        val accessToken = JWT(authTokens.accessToken)
        val refreshToken = JWT(authTokens.refreshToken)

        if (!accessToken.isExpired() && !forceRefreshAccessToken) {
            logger.debug(
                message = "using still valid access token",
                tag = Logger.AUTH_TAG
            )
            return accessToken.toString()
        }

        return if (refreshToken.isExpired()) {
            logger.debug(
                message = "both access or refresh tokens are expired, fallback to refreshing session",
                tag = Logger.AUTH_TAG
            )
            renewSessionAuthTokens()
        } else {
            logger.debug(
                message = "using still valid refresh token to refresh tokens",
                tag = Logger.AUTH_TAG
            )
            refreshSessionAuthTokens(refreshToken.toString())
        }.also { freshTokens ->
            if (JWT(freshTokens.accessToken).isExpired()) {
                throw NablaException.Authentication.UnableToGetFreshSessionToken(
                    IllegalStateException("access token is expired")
                )
            }
        }.accessToken
    }

    override fun clearSession() {
        sessionTokenProvider = null
        tokenLocalDataSource.clear()
    }

    private suspend fun renewSessionAuthTokens(): AuthTokens {
        val sessionTokenProvider =
            sessionTokenProvider ?: throw NablaException.Authentication.NotAuthenticated
        val patientId = patientRepository.getPatientId()
            ?: throw NablaException.Internal(RuntimeException("Session token provider available without patientId"))

        return sessionTokenProvider.fetchNewSessionAuthTokens(patientId)
            .getOrElse { exception ->
                throw NablaException.Authentication.UnableToGetFreshSessionToken(
                    exceptionMapper.map(exception)
                )
            }
            .also {
                tokenLocalDataSource.setAuthTokens(it)
            }
    }

    private suspend fun refreshSessionAuthTokens(refreshToken: String): AuthTokens {
        return runCatchingCancellable {
            val freshTokens = tokenRemoteDataSource.refresh(refreshToken)
            tokenLocalDataSource.setAuthTokens(freshTokens)
            logger.debug(
                message = "tokens refreshed, using refreshed access token",
                tag = Logger.AUTH_TAG
            )

            freshTokens
        }.getOrElse { refreshTokenError ->
            // Fallback to renewing session
            logger.debug(
                message = "fail refresh tokens, fallback to renew session",
                tag = Logger.AUTH_TAG
            )

            return runCatchingCancellable {
                renewSessionAuthTokens()
            }.getOrElse { renewSessionError ->
                // Ensure no exception is suppressed
                logger.debug(message = "fail renewing session", tag = Logger.AUTH_TAG)
                throw renewSessionError.apply { addSuppressed(refreshTokenError) }
            }
        }
    }
}

private fun JWT.isExpired(): Boolean {
    return this.isExpired(1)
}
