package com.nabla.sdk.core.data.auth

import android.util.Base64
import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.data.stubs.JwtFaker
import com.nabla.sdk.core.data.stubs.StdLogger
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.AccessToken
import com.nabla.sdk.core.domain.entity.AuthTokens
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.RefreshToken
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.domain.entity.toId
import com.nabla.sdk.tests.common.BaseCoroutineTest
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionClientImplTest : BaseCoroutineTest() {

    private val tokenLocalDataSource = mockk<TokenLocalDataSource>()
    private val tokenRemoteDataSource = mockk<TokenRemoteDataSource>()
    private val patientRepository = mockk<PatientRepository>()
    private val sessionTokenProvider = mockk<SessionTokenProvider>()
    private val sessionClient = SessionClientImpl(
        tokenLocalDataSource = tokenLocalDataSource,
        tokenRemoteDataSource = tokenRemoteDataSource,
        patientRepository = patientRepository,
        logger = StdLogger(),
        exceptionMapper = NablaExceptionMapper(),
        sessionTokenProvider = sessionTokenProvider,
    )

    @Test(expected = AuthenticationException.UserIdNotSet::class)
    fun `get fresh access token without patient should throw`() = runTest {
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { patientRepository.getPatientId() } returns null
        sessionClient.getFreshAccessToken(false)
    }

    @Test
    fun `get fresh access token authenticated with patient should succeed`() = runTest {
        val authTokens = AuthTokens.fake()
        val patientId = StringId(uuid4().toString())

        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { patientRepository.getPatientId() } returns patientId
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId.value) } returns Result.success(
            authTokens,
        )

        assertTrue {
            sessionClient.getFreshAccessToken(false) == authTokens.accessToken.token
        }
        verify { tokenLocalDataSource.setAuthTokens(authTokens) }
    }

    @Test
    fun `get fresh access token with valid access token should return it`() = runTest {
        val accessToken = JwtFaker.expiredIn2050
        every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(AccessToken(accessToken), RefreshToken(JwtFaker.expiredIn2050_2))
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
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { tokenRemoteDataSource.refresh(refreshToken) } returns AuthTokens(
                AccessToken(refreshedAccessToken),
                RefreshToken(refreshToken),
            )
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
                AccessToken(JwtFaker.expiredIn2050_2),
                RefreshToken(JwtFaker.expiredIn2050),
            )
            every { patientRepository.getPatientId() } returns patientId
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId.value) } returns Result.success(
                newSessionTokens,
            )
            assertTrue {
                sessionClient.getFreshAccessToken(false) == newSessionTokens.accessToken.token
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
                AccessToken(JwtFaker.expiredIn2050_3),
                RefreshToken(JwtFaker.expiredIn2050),
            )
            every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
            every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
            coEvery { tokenRemoteDataSource.refresh(refreshToken) } returns newSessionTokens
            assertTrue {
                sessionClient.getFreshAccessToken(true) == newSessionTokens.accessToken.token
            }
            verify {
                patientRepository wasNot Called
                sessionTokenProvider wasNot Called
            }
        }

    @Test
    fun `marking tokens as expired invalidates local token and is recoverable`() = runTest {
        val accessToken = JwtFaker.expiredIn2050
        val refreshToken = JwtFaker.expiredIn2050_2
        val newSessionTokens = AuthTokens(
            AccessToken(JwtFaker.expiredIn2050_3),
            RefreshToken(JwtFaker.expiredIn2050),
        )
        every { tokenLocalDataSource.getAuthTokens() } returns AuthTokens(AccessToken(accessToken), RefreshToken(refreshToken))
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs
        every { tokenLocalDataSource.clear() } just Runs
        every { patientRepository.getPatientId() } returns StringId("id")
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(any()) } returns Result.success(newSessionTokens)
        coEvery { tokenRemoteDataSource.refresh(refreshToken) } returns newSessionTokens

        assertEquals(accessToken, sessionClient.getFreshAccessToken())

        sessionClient.markTokensAsInvalid()
        every { tokenLocalDataSource.getAuthTokens() } returns null

        assertEquals(newSessionTokens.accessToken.token, sessionClient.getFreshAccessToken())
        coVerify {
            sessionTokenProvider.fetchNewSessionAuthTokens(any())
        }
    }

    @Test
    fun `expired access token from session provider should be refreshed`() = runTest {
        val patientId = StringId(uuid4().toString())

        // expired access token but valid refresh token
        val authTokens_1 = AuthTokens(
            AccessToken(JwtFaker.expiredIn2020),
            RefreshToken(JwtFaker.expiredIn2050),
        )
        // valid access and refresh tokens
        val authTokens_2 = AuthTokens(
            AccessToken(JwtFaker.expiredIn2050_3),
            RefreshToken(JwtFaker.expiredIn2050_2),
        )
        every { patientRepository.getPatientId() } returns patientId
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId.value) } returns Result.success(
            authTokens_1,
        ) andThen Result.success(authTokens_2)
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs

        assertTrue {
            sessionClient.getFreshAccessToken() == authTokens_2.accessToken.token
        }
    }

    @Test(expected = AuthenticationException.UnableToGetFreshSessionToken::class)
    fun `expired auth tokens from session provider should throw`() = runTest {
        val patientId = StringId(uuid4().toString())
        val expiredAuthTokens = AuthTokens(
            AccessToken(JwtFaker.expiredIn2020_2),
            RefreshToken(JwtFaker.expiredIn2020),
        )
        every { patientRepository.getPatientId() } returns patientId
        coEvery { sessionTokenProvider.fetchNewSessionAuthTokens(patientId.value) } returns Result.success(
            expiredAuthTokens,
        )
        every { tokenLocalDataSource.getAuthTokens() } returns null
        every { tokenLocalDataSource.setAuthTokens(any()) } just Runs

        sessionClient.getFreshAccessToken()
    }

    @Test
    fun `authenticatable flow emits error when starting with no user id set`() = runTest {
        val patientIdFlow = flowOf(null)
        every { patientRepository.getPatientIdFlow() } returns patientIdFlow

        val authenticatableFlow = sessionClient.authenticatableFlow(flowOf("Data from authenticated source"))

        authenticatableFlow.test {
            assertTrue { awaitError() is AuthenticationException.UserIdNotSet }
        }
    }

    @Test
    fun `authenticatable flow emits item when user id is set`() = runTest {
        val patientIdFlow = flowOf("patient-id".toId())
        every { patientRepository.getPatientIdFlow() } returns patientIdFlow

        val authenticatableFlow = sessionClient.authenticatableFlow(flowOf("1", "2"))

        authenticatableFlow.test {
            assertEquals("1", awaitItem())
            assertEquals("2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticatable flow emits error when user id is nullified`() = runTest {
        val patientIdFlow = MutableStateFlow<StringId?>(StringId("patient-id"))
        every { patientRepository.getPatientIdFlow() } returns patientIdFlow

        val authenticatableFlow = sessionClient.authenticatableFlow(flowOf("1"))

        authenticatableFlow.test {
            assertEquals("1", awaitItem())
            patientIdFlow.value = null
            assertTrue { awaitError() is AuthenticationException.UserIdNotSet }
        }
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
                JwtFaker.expiredIn2020,
            )
            assertTrue { jwts.distinct().size == jwts.size }
        }
    }
}
