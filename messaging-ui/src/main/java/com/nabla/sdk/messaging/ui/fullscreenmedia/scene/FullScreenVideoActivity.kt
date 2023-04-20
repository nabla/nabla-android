package com.nabla.sdk.messaging.ui.fullscreenmedia.scene

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.ui.helpers.DensityExtensions.dpToPx
import com.nabla.sdk.core.ui.helpers.IntentExtension.getSerializableExtraCompat
import com.nabla.sdk.messaging.ui.databinding.NablaActivityFullScreenVideoBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.sanitize
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import java.net.URI

// This activity currently does not support theme attributes customization
internal class FullScreenVideoActivity : AppCompatActivity() {
    private var binding: NablaActivityFullScreenVideoBinding? = null
    private var player: ExoPlayer? = null

    private val videoUri by lazy {
        (intent.getSerializableExtraCompat(VIDEO_URI_ARG, URI::class.java) ?: throwNablaInternalException("missing video uri arg"))
            .toAndroidUri()
            .sanitize()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = NablaActivityFullScreenVideoBinding.inflate(
            layoutInflater.cloneInContext(withNablaMessagingThemeOverlays()),
        )
        this.binding = binding
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()

        binding.fullScreenVideoBackButton.setOnClickListener {
            finish()
        }

        binding.fullScreenVideoBackButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                @Suppress("DEPRECATION")
                topMargin = windowInsets.systemWindowInsetTop + dpToPx(10)
            }

            windowInsets
        }

        player?.setMediaItem(MediaItem.fromUri(videoUri.sanitize()))
        player?.prepare()

        binding.fullScreenVideoPlayerView.player = player
    }

    override fun onDestroy() {
        binding = null
        player?.release()

        super.onDestroy()
    }

    companion object {
        fun newIntent(packageContext: Context, videoUri: URI): Intent =
            Intent(packageContext, FullScreenVideoActivity::class.java).apply {
                putExtra(VIDEO_URI_ARG, videoUri)
            }

        private const val VIDEO_URI_ARG = "videoUri"
    }
}
