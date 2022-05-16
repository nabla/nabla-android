package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.delay
import kotlin.time.Duration

suspend fun delayWithIdlingRes(idlingRes: CountingIdlingResource, duration: Duration) {
    idlingRes.increment()
    try {
        delay(duration)
    } finally {
        idlingRes.decrement()
    }
}
