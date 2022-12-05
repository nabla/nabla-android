package com.nabla.sdk.tests.common.rules

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class CoroutinesUiThreadOverrideRule(private val dispatcher: CoroutineDispatcher) : TestRule {

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            Dispatchers.setMain(dispatcher)

            base.evaluate()

            Dispatchers.resetMain()
        }
    }
}
