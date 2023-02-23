package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.ColorIntOrStateList
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemAudioMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment
import com.nabla.sdk.messaging.ui.scene.messages.PlaybackProgress
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class AudioMessageContentBinder(
    @AttrRes contentTextAppearanceAttr: Int,
    @AttrRes surfaceColorAttr: Int,
    @DrawableRes progressBackgroundDrawableRes: Int,
    private val binding: NablaConversationTimelineItemAudioMessageBinding,
    private val onToggleAudioMessagePlay: (audioMessageUri: Uri) -> Unit,
) : MessageContentBinder<TimelineItem.Message.Audio>() {

    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentTextAppearanceAttr))

    private val surfaceColor: ColorIntOrStateList = binding.context.getThemeColor(surfaceColorAttr)

    init {
        binding.audioMessageProgress.progressDrawable = AppCompatResources.getDrawable(binding.context, progressBackgroundDrawableRes)

        val colorBubble = surfaceColor
        val colorOnBubble = contentAppearance.textColor

        binding.audioMessageTitle.setTextColor(colorOnBubble)
        binding.audioMessageSecondsText.setTextColor(colorOnBubble)
        binding.audioPlayPauseButton.backgroundTintList = colorOnBubble
        binding.audioPlayPauseButton.imageTintList = colorBubble.asColorStateList(binding.context)
    }

    override fun bind(messageId: String, item: TimelineItem.Message.Audio) {
        bind(item.uri, item.isPlaying, item.progress)
    }

    fun bind(
        audioUri: Uri,
        isPlaying: Boolean,
        progress: PlaybackProgress,
    ) {
        binding.audioMessageProgress.max = progress.totalDurationMillis?.toInt() ?: Int.MAX_VALUE
        val currentProgressMillis = binding.audioMessageProgress.progress
        val targetProgressMillis = progress.currentPositionMillis
        ObjectAnimator.ofInt(binding.audioMessageProgress, "progress", targetProgressMillis.toInt())
            .apply {
                duration = if (targetProgressMillis > currentProgressMillis) ConversationFragment.AUDIO_PLAYBACK_PROGRESS_POLLING_MS else 0L
                interpolator = LinearInterpolator()
                setAutoCancel(true)
                start()
            }

        val durationToShowSeconds = progress.totalDurationMillis?.let { totalDuration ->
            (totalDuration - targetProgressMillis) / 1_000 // millis to seconds
        } ?: 0
        val minutes = (durationToShowSeconds / 60).toInt()
        val seconds = (durationToShowSeconds % 60).toInt()
        binding.audioMessageSecondsText.text = binding.context.getString(R.string.nabla_conversation_audio_message_seconds_format, minutes, seconds)

        binding.audioPlayPauseButton.setImageResource(if (isPlaying) R.drawable.nabla_ic_pause_audio else R.drawable.nabla_ic_play_audio)
        binding.audioPlayPauseButton.setOnClickListener { onToggleAudioMessagePlay(audioUri) }
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            @AttrRes surfaceColorAttr: Int,
            @DrawableRes progressBackgroundDrawableRes: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
            onToggleAudioMessagePlay: (audioMessageUri: Uri) -> Unit,
        ): AudioMessageContentBinder {
            return AudioMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                surfaceColorAttr = surfaceColorAttr,
                progressBackgroundDrawableRes = progressBackgroundDrawableRes,
                binding = NablaConversationTimelineItemAudioMessageBinding.inflate(inflater, parent, true),
                onToggleAudioMessagePlay = onToggleAudioMessagePlay
            )
        }
    }
}
