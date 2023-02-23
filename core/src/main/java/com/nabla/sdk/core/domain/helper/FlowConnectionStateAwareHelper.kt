package com.nabla.sdk.core.domain.helper

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.EventsConnectionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart

@NablaInternal
public object FlowConnectionStateAwareHelper {
    @OptIn(ExperimentalCoroutinesApi::class)
    @NablaInternal
    public fun <T> Flow<T>.restartWhenConnectionReconnects(
        eventsConnectionStateFlow: Flow<EventsConnectionState>,
    ): Flow<T> {
        var wasDisconnected = false
        var firstEmit = true

        return eventsConnectionStateFlow
            .onStart {
                firstEmit = true
            }
            .filter {
                // Always emit the first value to start the wrapped flow
                if (firstEmit) {
                    firstEmit = false
                    return@filter true
                }

                // If we are connected now and we were disconnected before, emit a new value to restart the wrapped flow
                if (it is EventsConnectionState.Connected && wasDisconnected) {
                    wasDisconnected = false
                    return@filter true
                }

                if (it is EventsConnectionState.Disconnected) {
                    wasDisconnected = true
                }

                return@filter false
            }
            .flatMapLatest { this }
    }
}
