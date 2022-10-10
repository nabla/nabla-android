package com.nabla.sdk.tests.common

import com.nabla.sdk.tests.common.rules.CoroutinesUiThreadOverrideRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.rules.TestRule

abstract class BaseCoroutineTest {
    protected val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutinesUIThreadOverrideRule: TestRule = CoroutinesUiThreadOverrideRule(testDispatcher)

    protected fun runTest(testBody: suspend TestScope.() -> Unit) {
        kotlinx.coroutines.test.runTest(
            context = testDispatcher,
            testBody = testBody,
        )
    }
}
