package com.nabla.sdk.core.data.auth

import com.auth0.android.jwt.JWT
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.AUTH_DOMAIN
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
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
            domain = AUTH_DOMAIN,
            message = "get fresh access token with forceRefreshAccessToken=$forceRefreshAccessToken...",
        )
        val authTokens = tokenLocalDataSource.getAuthTokens() ?: renewSessionAuthTokens()
        val accessToken = JWT(authTokens.accessToken)
        val refreshToken = JWT(authTokens.refreshToken)

        if (!accessToken.isExpired() && !forceRefreshAccessToken) {
            logger.debug(
                domain = AUTH_DOMAIN,
                message = "using still valid access token",
            )
            return accessToken.toString()
        }

        return if (refreshToken.isExpired()) {
            logger.debug(
                domain = AUTH_DOMAIN,
                message = "both access or refresh tokens are expired, fallback to refreshing session",
            )
            renewSessionAuthTokens()
        } else {
            logger.debug(
                domain = AUTH_DOMAIN,
                message = "using still valid refresh token to refresh tokens",
            )
            refreshSessionAuthTokens(refreshToken.toString())
        }.also { freshTokens ->
            if (JWT(freshTokens.accessToken).isExpired()) {
                throw AuthenticationException.UnableToGetFreshSessionToken(
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
            sessionTokenProvider ?: throw AuthenticationException.NotAuthenticated
        val patientId = patientRepository.getPatientId()
            ?: throwNablaInternalException("Session token provider available without patientId")

        return sessionTokenProvider.fetchNewSessionAuthTokens(patientId)
            .getOrElse { exception ->
                throw AuthenticationException.UnableToGetFreshSessionToken(
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
                domain = AUTH_DOMAIN,
                message = "tokens refreshed, using refreshed access token",
            )

            freshTokens
        }.getOrElse { refreshTokenError ->
            // Fallback to renewing session
            logger.debug(
                domain = AUTH_DOMAIN,
                message = "fail refresh tokens, fallback to renew session",
                error = refreshTokenError,
            )

            return runCatchingCancellable {
                renewSessionAuthTokens()
            }.getOrElse { renewSessionError ->
                // Ensure no exception is suppressed
                logger.debug(
                    domain = AUTH_DOMAIN,
                    message = "fail renewing session",
                    error = renewSessionError,
                )
                throw renewSessionError.apply { addSuppressed(refreshTokenError) }
            }
        }
    }
}

private fun JWT.isExpired(): Boolean {
    return this.isExpired(1)
}
