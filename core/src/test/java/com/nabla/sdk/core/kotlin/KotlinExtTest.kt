package com.nabla.sdk.core.kotlin

import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.nabla.sdk.tests.common.BaseCoroutineTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class KotlinExtTest : BaseCoroutineTest() {

    @Test(expected = CancellationException::class)
    fun `runCatchingCancellable rethrow cancellation exception`() = runTest {
        runCatchingCancellable {
            throw CancellationException()
        }
    }

    @Test
    fun `runCatchingCancellable is suspendable`() = runTest {
        runCatchingCancellable {
            delay(10_000)
        }
    }

    @Test
    fun `runCatchingCancellable catch non cancellation exception`() = runTest {
        assertTrue {
            runCatchingCancellable {
                throw IllegalArgumentException()
            }.isFailure
        }
    }

    @Test
    fun `sharedSingleIn join new request if computation is not completed`() = runTest {
        val job = Job()
        val sharedSingleIn = sharedSingleIn(this + job) {
            delay(10_000)
            uuid4()
        }
        val firstDef = async { sharedSingleIn.await() }
        delay(9_999)
        val secondDef = async { sharedSingleIn.await() }
        assertEquals(firstDef.await(), secondDef.await())
        job.cancel()
    }

    @Test
    // this level of precision requires an EmptyCoroutineContext
    fun `sharedSingleIn join new request if it occurs when computation completes`() = kotlinx.coroutines.test.runTest(EmptyCoroutineContext) {
        val job = Job()
        val sharedSingleIn = sharedSingleIn(this + job) {
            delay(10_000)
            uuid4()
        }
        val firstDef = async { sharedSingleIn.await() }
        delay(10_000)
        val secondDef = async { sharedSingleIn.await() }
        assertEquals(firstDef.await(), secondDef.await())
        job.cancel()
    }

    @Test
    fun `sharedSingleIn don't join new request if it occurs after computation completed`() = runTest {
        val job = Job()
        val sharedSingleIn = sharedSingleIn(this + job) {
            delay(10_000)
            uuid4()
        }
        val firstDef = async { sharedSingleIn.await() }
        delay(10_001)
        val secondDef = async { sharedSingleIn.await() }
        assertNotEquals(firstDef.await(), secondDef.await())
        job.cancel()
    }

    @Test
    fun `shareInWithMaterializedErrors materializes errors`() = runTest {
        val job = Job()
        val erroringFlow =
            flow {
                emit(Unit)
                throw IllegalArgumentException()
            }.shareInWithMaterializedErrors(
                this + job,
                started = SharingStarted.WhileSubscribed(),
            )
        erroringFlow.test {
            awaitItem()
            awaitError()
        }
        job.cancel()
    }
}
