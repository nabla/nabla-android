package com.nabla.sdk.core.data.auth

import android.util.Base64
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.stubs.JwtFaker
import com.nabla.sdk.core.data.stubs.StdLogger
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.InternalException
import com.nabla.sdk.core.domain.entity.StringId
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionClientImplTest {

    private val tokenLocalDataSource = mockk<TokenLocalDataSource>()
    private val tokenRemoteDataSource = mockk<TokenRemoteDataSource>()
    private val patientRepository = mockk<PatientRepository>()
    private val sessionTokenProvider = mockk<SessionTokenProvider>()
    private val sessionClient = SessionClientImpl(
        tokenLocalDataSource = tokenLocalDataSource,
        tokenRemoteDataSource = tokenRemoteDataSource,
        patientRepository = patientRepository,
        logger = StdLogger(),
        exceptionMapper = NablaExceptionMapper()
    )

    @Test(expected = AuthenticationException.NotAuthenticated::class)
    fun `get fresh access token while unauthenticated should throw`() = runTest {
        every { tokenLocalDataSource.getAuthTokens() } returns null
        sessionClient.getFreshAccessToken(false)
    }

    @Test(expected = InternalException::class)
    fun `get fresh access token authenticated but without patient should throw`() = runTest {
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { patientRepository.getPatientId() } returns null
        sessionClient.initSession {
            Result.success(AuthTokens.fake())
        }
        sessionClient.getFreshAccessToken(false)
    }

    @Test
    fun `get fresh access token authenticated with patient should succeed`() = runTest {
        val authTokens = AuthTokens.fake()
        val patientId = StringId(uuid4().toString())

        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { patientRepository.getPatientId() } returns patientId
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId) } returns Result.success(
            authTokens
        )

        sessionClient.initSession(sessionTokenProvider)
        assertTrue {
            sessionClient.getFreshAccessToken(false) == authTokens.accessToken
        }
        verify { tokenLocalDataSource.setAuthTokens(authTokens) }
    }

    @Test
    fun `get fresh access token with valid access token should return it`() = runTest {
        val accessToken = JwtFaker.expiredIn2050
        every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(refreshToken = JwtFaker.expiredIn2050_2, accessToken = accessToken)
        sessionClient.initSession(sessionTokenProvider)
        assertTrue {
            sessionClient.getFreshAccessToken(false) == accessToken
        }
        verify {
            tokenRemoteDataSource wasNot Called
            patientRepository wasNot Called
            sessionTokenProvider wasNot Called
        }
    }

    @Test
    fun `get fresh access token with expired access token should return refreshed tokens using refresh token`() =
        runTest {
            val accessToken = JwtFaker.expiredIn2020
            val refreshToken = JwtFaker.expiredIn2050
            val refreshedAccessToken = JwtFaker.expiredIn2050_2
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(refreshToken = refreshToken, accessToken = accessToken)
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { tokenRemoteDataSource.refresh(refreshToken) } returns AuthTokens(
                refreshToken = refreshToken,
                accessToken = refreshedAccessToken
            )
            sessionClient.initSession(sessionTokenProvider)
            assertTrue {
                sessionClient.getFreshAccessToken(false) == refreshedAccessToken
            }
            verify {
                patientRepository wasNot Called
                sessionTokenProvider wasNot Called
            }
        }

    @Test
    fun `get fresh access token with expired access and refresh tokens should return refreshed session`() =
        runTest {
            val patientId = StringId(uuid4().toString())
            val accessToken = JwtFaker.expiredIn2020
            val refreshToken = JwtFaker.expiredIn2020
            val newSessionTokens = AuthTokens(
                refreshToken = JwtFaker.expiredIn2050,
                accessToken = JwtFaker.expiredIn2050_2
            )
            every { patientRepository.getPatientId() } returns patientId
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(refreshToken = refreshToken, accessToken = accessToken)
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId) } returns Result.success(
                newSessionTokens
            )
            sessionClient.initSession(sessionTokenProvider)
            assertTrue {
                sessionClient.getFreshAccessToken(false) == newSessionTokens.accessToken
            }
            verify {
                tokenRemoteDataSource wasNot Called
            }
        }

    @Test
    fun `get forced fresh access token with valid access token still refresh access token`() =
        runTest {
            val accessToken = JwtFaker.expiredIn2050
            val refreshToken = JwtFaker.expiredIn2050_2
            val newSessionTokens = AuthTokens(
                refreshToken = JwtFaker.expiredIn2050,
                accessToken = JwtFaker.expiredIn2050_3
            )
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(refreshToken = refreshToken, accessToken = accessToken)
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { tokenRemoteDataSource.refresh(refreshToken) } returns newSessionTokens
            sessionClient.initSession(sessionTokenProvider)
            assertTrue {
                sessionClient.getFreshAccessToken(true) == newSessionTokens.accessToken
            }
            verify {
                patientRepository wasNot Called
                sessionTokenProvider wasNot Called
            }
        }

    @Test
    fun `expired access token from session provider should be refreshed`() = runTest {
        val patientId = StringId(uuid4().toString())

        // expired access token but valid refresh token
        val authTokens_1 = AuthTokens(
            refreshToken = JwtFaker.expiredIn2050,
            accessToken = JwtFaker.expiredIn2020
        )
        // valid access and refresh tokens
        val authTokens_2 = AuthTokens(
            refreshToken = JwtFaker.expiredIn2050_2,
            accessToken = JwtFaker.expiredIn2050_3
        )
        every { patientRepository.getPatientId() } returns patientId
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId) } returns Result.success(
            authTokens_1
        ) andThen Result.success(authTokens_2)
        sessionClient.initSession(sessionTokenProvider)
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs

        assertTrue {
            sessionClient.getFreshAccessToken() == authTokens_2.accessToken
        }
    }

    @Test(expected = AuthenticationException.UnableToGetFreshSessionToken::class)
    fun `expired auth tokens from session provider should throw`() = runTest {
        val patientId = StringId(uuid4().toString())
        val expiredAuthTokens = AuthTokens(
            refreshToken = JwtFaker.expiredIn2020,
            accessToken = JwtFaker.expiredIn2020_2
        )
        every { patientRepository.getPatientId() } returns patientId
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId) } returns Result.success(
            expiredAuthTokens
        )
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
        sessionClient.initSession(sessionTokenProvider)

        sessionClient.getFreshAccessToken()
    }

    @Before
    fun before() {
        mockAndroidBase64()
    }

    @After
    fun after() {
        clearAllMocks()
    }

    private fun mockAndroidBase64() {
        mockkStatic(Base64::class)

        val stringSlot = slot<String>()
        every {
            Base64.decode(capture(stringSlot), any())
        } answers {
            java.util.Base64.getDecoder().decode(stringSlot.captured)
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            val jwts = listOf(
                JwtFaker.expiredIn2050,
                JwtFaker.expiredIn2050_2,
                JwtFaker.expiredIn2050_3,
                JwtFaker.expiredIn2020
            )
            assertTrue { jwts.distinct().size == jwts.size }
        }
    }
}
