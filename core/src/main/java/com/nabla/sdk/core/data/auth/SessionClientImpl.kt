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
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class SessionClientImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val tokenRemoteDataSource: TokenRemoteDataSource,
    private val patientRepository: PatientRepository,
    private val logger: Logger,
    private val exceptionMapper: NablaExceptionMapper,
    private val sessionTokenProvider: SessionTokenProvider,
) : SessionClient {

    private val refreshLock = Mutex()

    override fun authenticatableOrThrow() {
        patientRepository.getPatientId() ?: throw AuthenticationException.UserIdNotSet()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun <T> authenticatableFlow(flow: Flow<T>): Flow<T> {
        return patientRepository.getPatientIdFlow()
            .distinctUntilChanged()
            .flatMapLatest { patientId ->
                if (patientId == null) {
                    throw AuthenticationException.UserIdNotSet()
                }
                flow
            }
    }

    override suspend fun getFreshAccessToken(forceRefreshAccessToken: Boolean): String {
        return refreshLock.withLock {
            getFreshAccessTokenUnsafe(forceRefreshAccessToken)
        }
    }

    override fun markTokensAsInvalid() {
        tokenLocalDataSource.clear()
    }

    private suspend fun getFreshAccessTokenUnsafe(
        forceRefreshAccessToken: Boolean,
    ): String {
        logger.debug(
            domain = AUTH_DOMAIN,
            message = "get fresh access token with forceRefreshAccessToken=$forceRefreshAccessToken...",
        )
        val authTokens = tokenLocalDataSource.getAuthTokens() ?: renewSessionAuthTokens()
        val accessToken = JWT(authTokens.accessToken.token)
        val refreshToken = JWT(authTokens.refreshToken.token)

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
            if (JWT(freshTokens.accessToken.token).isExpired()) {
                throw AuthenticationException.UnableToGetFreshSessionToken(
                    IllegalStateException("access token is expired"),
                )
            }
        }.accessToken.token
    }

    private suspend fun renewSessionAuthTokens(): AuthTokens {
        val patientId = patientRepository.getPatientId() ?: throw AuthenticationException.UserIdNotSet()

        return sessionTokenProvider.fetchNewSessionAuthTokens(patientId.value)
            .getOrElse { exception ->
                throw AuthenticationException.UnableToGetFreshSessionToken(
                    exceptionMapper.map(exception),
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
