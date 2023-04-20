package com.nabla.sdk.core.data.apollo

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.CLOSE_GOING_AWAY
import com.apollographql.apollo3.network.ws.CLOSE_NORMAL
import com.apollographql.apollo3.network.ws.WebSocketConnection
import com.apollographql.apollo3.network.ws.WebSocketEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

/**
 * A [WebSocketEngine] that exposes a [WebSocketConnectionState].
 *
 * The implementation is a bit naive in the sense that it considers that it only exposes the status
 * of the latest created [WebSocketConnection] so if someday we use multiple connections (at different
 * URLs for example) it wouldn't work anymore.
 *
 * The code is copied from "com.apollographql.apollo3.network.ws.OkHttpWebSocketEngine"
 */
internal class ConnectionStateAwareWebsocketEngine(
    private val webSocketFactory: WebSocket.Factory,
    private val clock: Clock,
) : WebSocketEngine {
    internal sealed class WebSocketConnectionState {
        object NotConnected : WebSocketConnectionState()
        object Connecting : WebSocketConnectionState()
        data class Disconnected(val since: Instant) : WebSocketConnectionState()
        object Connected : WebSocketConnectionState()
    }

    private var disconnectedSince: Instant? = null

    private val connectionStateMutableStateFlow = MutableStateFlow<WebSocketConnectionState>(WebSocketConnectionState.NotConnected)
    val connectionStateFlow: StateFlow<WebSocketConnectionState> = connectionStateMutableStateFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun open(url: String, headers: List<HttpHeader>): WebSocketConnection {
        val messageChannel = Channel<String>(Channel.UNLIMITED)
        val webSocketOpenResult = CompletableDeferred<Unit>()

        val request = Request.Builder()
            .url(url)
            .headers(headers.toOkHttpHeaders())
            .build()

        onWebsocketConnecting()

        val webSocket = webSocketFactory.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    onWebsocketConnected()
                    webSocketOpenResult.complete(Unit)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    messageChannel.trySend(text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    messageChannel.trySend(bytes.utf8())
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onWebsocketDisconnected()
                    webSocketOpenResult.complete(Unit)
                    messageChannel.close(t)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    onWebsocketDisconnected()
                    webSocketOpenResult.complete(Unit)

                    val t = ApolloWebSocketClosedException(code, reason)
                    messageChannel.close(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    onWebsocketDisconnected()
                    messageChannel.close()
                }
            },
        )

        webSocketOpenResult.await()

        messageChannel.invokeOnClose {
            // I think this is not necessary. The caller must call [WebSocketConnection.close] in all cases.
            // This should either trigger onClose or onFailure which should close the messageChannel
            //
            // Since this is idempotent, it shouldn't harm too much to keep it
            webSocket.close(CLOSE_GOING_AWAY, null)
        }

        return object : WebSocketConnection {
            override suspend fun receive(): String {
                return messageChannel.receive()
            }

            override fun send(data: ByteString) {
                if (!webSocket.send(data)) {
                    // The websocket is full or closed
                    messageChannel.close()
                }
            }

            override fun send(string: String) {
                if (!webSocket.send(string)) {
                    // The websocket is full or closed
                    messageChannel.close()
                }
            }

            override fun close() {
                webSocket.close(CLOSE_NORMAL, null)
            }
        }
    }

    @Deprecated(
        "Use open(String, List<HttpHeader>) instead.",
        replaceWith = ReplaceWith(
            "open(url, headers.map { HttpHeader(it.key, it.value })",
            "com.apollographql.apollo3.api.http.HttpHeader",
        ),
    )
    override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection {
        return open(url, headers.map { HttpHeader(it.key, it.value) })
    }

    private fun List<HttpHeader>.toOkHttpHeaders(): Headers =
        Headers.Builder().also { headers ->
            this.forEach {
                headers.add(it.name, it.value)
            }
        }.build()

    private fun onWebsocketConnecting() {
        if (connectionStateMutableStateFlow.value != WebSocketConnectionState.Connected) {
            connectionStateMutableStateFlow.value = WebSocketConnectionState.Connecting
        }
    }

    private fun onWebsocketConnected() {
        disconnectedSince = null
        connectionStateMutableStateFlow.value = WebSocketConnectionState.Connected
    }

    private fun onWebsocketDisconnected() {
        // Do not reset the Disconnected since value if it was already disconnected
        if (connectionStateMutableStateFlow.value !is WebSocketConnectionState.Disconnected) {
            val disconnectedSince = this.disconnectedSince
            if (disconnectedSince != null) {
                connectionStateMutableStateFlow.value = WebSocketConnectionState.Disconnected(since = disconnectedSince)
            } else {
                val now = clock.now()
                this.disconnectedSince = now
                connectionStateMutableStateFlow.value = WebSocketConnectionState.Disconnected(since = now)
            }
        }
    }
}
