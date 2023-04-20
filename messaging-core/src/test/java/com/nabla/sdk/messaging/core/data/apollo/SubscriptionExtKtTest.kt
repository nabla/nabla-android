package com.nabla.sdk.messaging.core.data.apollo

import app.cash.turbine.test
import com.nabla.sdk.core.data.stubs.TestClock
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.tests.common.BaseCoroutineTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionExtKtTest : BaseCoroutineTest() {
    @Test
    fun `notifyTypingUpdates() notifies typing updates`() = runTest {
        val clock = TestClock(this)
        val typingProvider = ProviderInConversation(
            Provider.fake(),
            clock.now(),
            null,
        )
        val eventFlowWithProviders = flow {
            emit(typingProvider)
        }.notifyTypingUpdates(clock, coroutineContext) {
            listOf(it)
        }
        eventFlowWithProviders.test {
            assertTrue(awaitItem().isTyping(clock))
            assertFalse(awaitItem().isTyping(clock))
            awaitComplete()
        }
    }
}
