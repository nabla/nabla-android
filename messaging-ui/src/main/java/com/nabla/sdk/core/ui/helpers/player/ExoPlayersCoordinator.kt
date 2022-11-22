package com.nabla.sdk.core.ui.helpers.player

import android.content.Context
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes

// we want the fragment lifecycle here, view lifecycle is too short
internal fun Fragment.createMediaPlayersCoordinator(contentType: Int = C.AUDIO_CONTENT_TYPE_MOVIE, pauseOnPause: Boolean = true): ExoPlayersCoordinator {
    val coordinator = ExoPlayersCoordinator(contentType)
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            coordinator.clearAll()
            lifecycle.removeObserver(this)
        }

        override fun onPause(owner: LifecycleOwner) {
            if (pauseOnPause) {
                coordinator.pauseAll()
            }
        }
    })
    return coordinator
}

/**
 * Helps maintain & coordinate a pool of video/audio players to never have more than one playing at a time.
 */
internal class ExoPlayersCoordinator internal constructor(private val contentType: Int = C.AUDIO_CONTENT_TYPE_MOVIE) {
    private val exoPlayersPool = mutableMapOf<Uri, Player>()

    fun getAll(): Map<Uri, Player> = exoPlayersPool.toMap()

    fun getOrCreatePlayerForUri(context: Context, uri: Uri, onCreated: Player.() -> Unit = {}): Player {
        return exoPlayersPool.getOrPut(uri) { createPlayer(context, uri).apply(onCreated) }
    }

    fun getPlayerForUri(uri: Uri): Player? = exoPlayersPool[uri]

    private fun createPlayer(context: Context, uri: Uri) =
        ExoPlayer
            .Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(contentType)
                        .build(),
                    true, // handleAudioFocus
                )
                prepare()
                addListener(
                    object : Player.Listener {
                        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                            if (playWhenReady) {
                                // when playback is started, pause any other ongoing playback.
                                pauseAllExcept(uri)
                            }
                        }
                    }
                )
            }

    fun clearPlayersWhere(playerCanBeClearedPredicate: (Uri) -> Boolean) {
        exoPlayersPool.forEach { (uri, player) ->
            if (playerCanBeClearedPredicate(uri)) {
                player.release()
                exoPlayersPool.remove(uri)
            }
        }
    }

    fun clearAll() {
        exoPlayersPool.forEach { (_, player) -> player.release() }
        exoPlayersPool.clear()
    }

    internal fun pauseAll() {
        exoPlayersPool.forEach { (_, player) ->
            player.pause()
        }
    }

    private fun pauseAllExcept(excludedUri: Uri) {
        exoPlayersPool.forEach { (uri, player) ->
            if (uri != excludedUri) {
                player.playWhenReady = false
            }
        }
    }
}
