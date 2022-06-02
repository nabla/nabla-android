package com.nabla.sdk.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TestClock(private val testScope: TestScope) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(testScope.currentTime)
}
