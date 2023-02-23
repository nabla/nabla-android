package com.nabla.sdk.core.domain.helper

import app.cash.turbine.test
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import com.nabla.sdk.core.domain.helper.FlowConnectionStateAwareHelper.restartWhenConnectionReconnects
import com.nabla.sdk.tests.common.BaseCoroutineTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FlowConnectionStateAwareHelperTest : BaseCoroutineTest() {

    @Test
    fun `test we are always emitting the first value`() = runTest {
        flowOf(1)
            .restartWhenConnectionReconnects(flow { emit(EventsConnectionState.Disconnected(since = Clock.System.now())) })
            .test {
                assertEquals(1, awaitItem())
                expectNoEvents()
            }
    }

    @Test
    fun `test we are restarting the flow when we disconnect and then reconnect`() = runTest {
        flow {
            emit(1)
            emit(2)
        }
            .restartWhenConnectionReconnects(
                flow {
                    emit(EventsConnectionState.Connected)
                    emit(EventsConnectionState.Disconnected(since = Clock.System.now()))
                    emit(EventsConnectionState.Connected)
                }
            )
            .test {
                assertEquals(1, awaitItem())
                assertEquals(2, awaitItem())
                assertEquals(1, awaitItem())
                assertEquals(2, awaitItem())

                expectNoEvents()
            }
    }

    @Test
    fun `test we are not restarting the flow when we don't disconnect`() = runTest {
        flow {
            emit(1)
            emit(2)
        }
            .restartWhenConnectionReconnects(
                flow {
                    emit(EventsConnectionState.Connected)
                    emit(EventsConnectionState.Connecting)
                    emit(EventsConnectionState.Connected)
                }
            )
            .test {
                assertEquals(1, awaitItem())
                assertEquals(2, awaitItem())

                expectNoEvents()
            }
    }
}
