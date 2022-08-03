package com.nabla.sdk.messaging.core.data.stubs

import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * For stub repository wanting to fake network latency, since network calls are supposed to increment idling res.
 */
suspend fun delayWithIdlingRes(idlingRes: CountingIdlingResource, duration: Duration) {
    idlingRes.increment()
    try {
        delay(duration)
    } finally {
        idlingRes.decrement()
    }
}
