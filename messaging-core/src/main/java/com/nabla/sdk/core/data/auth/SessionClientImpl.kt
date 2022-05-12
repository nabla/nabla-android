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
        val accessToken = tokenLocalDataSource.getAccessToken()
        if (accessToken.isValid() && !forceRefreshAccessToken) {
            logger.debug(
                message = "using still valid access token",
                tag = Logger.AUTH_TAG
            )
            return accessToken.toString()
        }
        val refreshToken = tokenLocalDataSource.getRefreshToken()
        if (refreshToken.isValid()) {
            logger.debug(
                message = "using still valid refresh token to refresh tokens",
                tag = Logger.AUTH_TAG
            )

            return runCatchingCancellable {
                val freshTokens = tokenRemoteDataSource.refresh(refreshToken.toString())
                tokenLocalDataSource.setAuthTokens(freshTokens)
                logger.debug(
                    message = "tokens refreshed, using refreshed access token",
                    tag = Logger.AUTH_TAG
                )

                freshTokens.accessToken
            }.getOrElse { refreshTokenError ->
                // Fallback to refreshing session
                logger.debug(
                    message = "fail refresh tokens, fallback to refresh session",
                    tag = Logger.AUTH_TAG
                )

                return runCatchingCancellable {
                    renewSessionAuthTokens().accessToken.also {
                        logger.debug(message = "success refresh session", tag = Logger.AUTH_TAG)
                    }
                }.getOrElse { renewSessionError ->
                    // Ensure no exception is suppressed
                    logger.debug(message = "fail refreshing session", tag = Logger.AUTH_TAG)
                    throw renewSessionError.apply { addSuppressed(refreshTokenError) }
                }
            }
        }
        // no access or refresh tokens are valid, fallback to refreshing session
        return renewSessionAuthTokens().accessToken
    }

    override fun clearSession() {
        sessionTokenProvider = null
        tokenLocalDataSource.clear()
    }

    private suspend fun renewSessionAuthTokens(): AuthTokens {
        val sessionTokenProvider = sessionTokenProvider ?: throw NablaException.Authentication.NotAuthenticated
        val patientId = patientRepository.getPatientId()
            ?: throw NablaException.Internal(RuntimeException("Session token provider available without patientId"))

        return sessionTokenProvider.fetchNewSessionAuthTokens(patientId)
            .getOrElse { exception -> throw NablaException.Authentication.UnableToGetFreshSessionToken(exceptionMapper.map(exception)) }
            .also {
                tokenLocalDataSource.setAuthTokens(it)
            }
    }
}

private fun JWT?.isValid(): Boolean {
    return this != null && !this.isExpired(1)
}
