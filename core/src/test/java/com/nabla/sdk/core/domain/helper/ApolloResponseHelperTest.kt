package com.nabla.sdk.core.domain.helper

import app.cash.turbine.test
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.CacheInfo
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.data.exception.NablaExceptionMapper
import com.nabla.sdk.core.domain.entity.AuthenticationException
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.helper.ApolloResponseHelper.makeCachedResponseWatcher
import com.nabla.sdk.tests.common.BaseCoroutineTest
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApolloResponseHelperTest : BaseCoroutineTest() {
    @Test
    fun `test watcher throwing without previous result propagates the error`() = runTest {
        val exception = Exception("Test")

        makeCachedResponseWatcher(
            exceptionMapper = NablaExceptionMapper(),
        ) {
            flow<ApolloResponse<Query.Data>> {
                throw exception
            }
        }.test {
            val error = awaitError()
            assertEquals(exception.message, error.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test watcher throwing network exception with a cached data sends the corresponding error + cached data as response`() = runTest {
        val exception = ApolloNetworkException("Network exception")

        makeCachedResponseWatcher(
            exceptionMapper = NablaExceptionMapper(),
        ) {
            flow {
                emit(generateResponse(isLast = false, isFromCache = true))
                throw exception
            }
        }.test {
            val firstResponse = awaitItem()
            assertEquals(false, firstResponse.isDataFresh)
            assertEquals(RefreshingState.Refreshing, firstResponse.refreshingState)
            assertNotNull(firstResponse.data)

            val secondResponse = awaitItem()
            assertEquals(false, secondResponse.isDataFresh)
            secondResponse.refreshingState.let {
                assertTrue(it is RefreshingState.ErrorWhileRefreshing)
                assertEquals(exception.message, it.error.message)
            }
            assertNotNull(secondResponse.data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test watcher throwing no user exception with cached data sends the error as response`() = runTest {
        val exception = AuthenticationException.UserIdNotSet()

        makeCachedResponseWatcher(
            exceptionMapper = NablaExceptionMapper(),
        ) {
            flow {
                emit(generateResponse(isLast = false, isFromCache = true))
                throw exception
            }
        }.test {
            val firstResponse = awaitItem()
            assertEquals(false, firstResponse.isDataFresh)
            assertEquals(RefreshingState.Refreshing, firstResponse.refreshingState)
            assertNotNull(firstResponse.data)

            val error = awaitError()
            assertTrue(error is AuthenticationException.UserIdNotSet)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test watcher with cache data and successful network refresh`() = runTest {
        makeCachedResponseWatcher(
            exceptionMapper = NablaExceptionMapper(),
        ) {
            flow {
                emit(generateResponse(isLast = false, isFromCache = true))
                emit(generateResponse(isLast = true, isFromCache = false))
            }
        }.test {
            val firstResponse = awaitItem()
            assertEquals(false, firstResponse.isDataFresh)
            assertEquals(RefreshingState.Refreshing, firstResponse.refreshingState)
            assertNotNull(firstResponse.data)

            val secondResponse = awaitItem()
            assertEquals(true, secondResponse.isDataFresh)
            assertEquals(RefreshingState.Refreshed, secondResponse.refreshingState)
            assertNotNull(secondResponse.data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun generateResponse(isLast: Boolean, isFromCache: Boolean): ApolloResponse<Query.Data> {
        return ApolloResponse.Builder(
            operation = fakeOperation,
            requestUuid = Uuid.randomUUID(),
            data = object : Query.Data {}
        )
            .isLast(isLast)
            .addExecutionContext(CacheInfo.Builder().cacheHit(isFromCache).build())
            .build()
    }

    companion object {
        private val fakeOperation: Operation<Query.Data> = mockk()
    }
}
