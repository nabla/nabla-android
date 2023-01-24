package com.nabla.sdk.core.domain.entity

import kotlinx.datetime.Instant

public sealed class EventsConnectionState {
    /**
     * Case when the connection hasn't been opened yet. Connection is opened only when needed so
     * it will stay in this state until you use the SDK in a way that requires events.
     */
    public object NotConnected : EventsConnectionState()

    /**
     * The SDK is currently trying to connect
     */
    public object Connecting : EventsConnectionState()

    /**
     * The SDK is connected and will receive events
     */
    public object Connected : EventsConnectionState()

    /**
     * The SDK has been disconnected, probably because of a network issue. It will automatically
     * try to reconnect.
     */
    public data class Disconnected(val since: Instant) : EventsConnectionState()
}
