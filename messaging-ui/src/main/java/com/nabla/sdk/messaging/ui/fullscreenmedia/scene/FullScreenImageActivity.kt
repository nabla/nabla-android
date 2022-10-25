package com.nabla.sdk.messaging.ui.fullscreenmedia.scene

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import coil.imageLoader
import coil.request.ImageRequest
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaActivityFullScreenImageBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.createSharableJpegImage
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.createSharingIntent
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.sanitize
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays
import java.net.URI

// This activity currently does not support theme attributes customization
internal class FullScreenImageActivity : AppCompatActivity() {
    private var binding: NablaActivityFullScreenImageBinding? = null

    private val imageUri by lazy {
        (intent.extras?.get(IMAGE_URI_ARG) as? URI ?: throwNablaInternalException("missing image uri arg"))
            .toAndroidUri()
            .sanitize()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = NablaActivityFullScreenImageBinding.inflate(
            LayoutInflater.from(applicationContext?.withNablaMessagingThemeOverlays())
        )
        this.binding = binding
        setContentView(binding.root)

        binding.fullScreenImageBackButton.setOnClickListener {
            finish()
        }

        binding.fullScreenImageBackButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                @Suppress("DEPRECATION")
                topMargin = windowInsets.systemWindowInsetTop + dpToPx(10)
            }

            windowInsets
        }

        binding.fullScreenImageShareButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                @Suppress("DEPRECATION")
                topMargin = windowInsets.systemWindowInsetTop + dpToPx(10)
            }

            windowInsets
        }

        binding.fullScreenImageProgressBar.isVisible = true
        binding.fullScreenImageTouchImageView.isVisible = false
        binding.fullScreenImageShareButton.isVisible = false

        binding.context.imageLoader.enqueue(
            ImageRequest.Builder(binding.context)
                .data(imageUri)
                .allowHardware(false) // https://stackoverflow.com/a/61893091/2508174
                .target(
                    onSuccess = { drawable ->
                        if (isFinishing) {
                            return@target
                        }

                        binding.fullScreenImageProgressBar.isVisible = false
                        binding.fullScreenImageTouchImageView.isVisible = true
                        binding.fullScreenImageTouchImageView.setImageDrawable(drawable)
                        binding.fullScreenImageShareButton.isVisible = true
                        binding.fullScreenImageShareButton.setOnClickListener {
                            try {
                                startActivity(
                                    Intent.createChooser(
                                        (drawable as BitmapDrawable).bitmap
                                            .createSharableJpegImage(imageUri.hashCode().toString(), binding.context)
                                            .createSharingIntent("image/jpeg"),
                                        getString(R.string.nabla_conversation_full_screen_image_sharing_chooser_title),
                                    )
                                )
                            } catch (_: Throwable) {
                                // TODO handle error
                            }
                        }
                    },
                )
                .listener(
                    onError = { _, _ ->
                        if (isFinishing) {
                            return@listener
                        }

                        binding.fullScreenImageProgressBar.isVisible = false
                        binding.fullScreenImageTouchImageView.isVisible = true
                        binding.fullScreenImageShareButton.isVisible = false

                        // TODO handle error
                    },
                )
                .build()
        )
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    companion object {
        fun newIntent(packageContext: Context, imageUri: URI): Intent =
            Intent(packageContext, FullScreenImageActivity::class.java).apply {
                putExtra(IMAGE_URI_ARG, imageUri)
            }

        private const val IMAGE_URI_ARG = "imageUri"
    }
}
