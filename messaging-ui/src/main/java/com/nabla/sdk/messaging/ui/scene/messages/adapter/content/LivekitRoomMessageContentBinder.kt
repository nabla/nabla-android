package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.view.isVisible
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.ui.helpers.ColorIntOrStateList
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemLivekitRoomMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class LivekitRoomMessageContentBinder(
    @AttrRes contentTextAppearanceAttr: Int,
    @AttrRes surfaceColorAttr: Int,
    private val binding: NablaConversationTimelineItemLivekitRoomMessageBinding,
    private val onJoinLivekitRoomClicked: (String, String, String) -> Unit,
) : MessageContentBinder<TimelineItem.Message.LivekitRoom>() {
    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentTextAppearanceAttr))
    private val surfaceColor: ColorIntOrStateList = binding.context.getThemeColor(surfaceColorAttr)

    override fun bind(messageId: String, item: TimelineItem.Message.LivekitRoom) {
        binding.title.setTextColor(contentAppearance.textColor)
        binding.subtitle.setTextColor(contentAppearance.textColor)
        binding.join.backgroundTintList = contentAppearance.textColor
        binding.join.setTextColor(surfaceColor.asColorStateList(binding.context))
        binding.icon.backgroundTintList = contentAppearance.textColor
        binding.icon.imageTintList = surfaceColor.asColorStateList(binding.context)

        when (item.roomStatus) {
            is TimelineItem.Message.LivekitRoom.Status.LivekitClosedRoom -> {
                binding.join.isVisible = false
                binding.subtitle.isVisible = true
                binding.subtitle.text = binding.context.getString(R.string.nabla_conversation_video_consultation_ended)
            }
            is TimelineItem.Message.LivekitRoom.Status.LivekitOpenedRoom -> {
                binding.join.isVisible = true
                binding.subtitle.isVisible = false
                binding.join.setOnClickListener {
                    onJoinLivekitRoomClicked(
                        item.roomStatus.url,
                        item.roomId.toString(),
                        item.roomStatus.token,
                    )
                }
                binding.join.text = if (item.roomStatus.isCurrentVideoCall) {
                    binding.context.getString(R.string.nabla_conversation_video_consultation_return_to_call)
                } else {
                    binding.context.getString(R.string.nabla_conversation_video_consultation_join)
                }
            }
        }
    }

    companion object {
        fun create(
            @AttrRes contentTextAppearanceAttr: Int,
            @AttrRes surfaceColorAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
            onJoinLivekitRoomClicked: (String, String, String) -> Unit,
        ): LivekitRoomMessageContentBinder {
            return LivekitRoomMessageContentBinder(
                contentTextAppearanceAttr = contentTextAppearanceAttr,
                surfaceColorAttr = surfaceColorAttr,
                binding = NablaConversationTimelineItemLivekitRoomMessageBinding.inflate(inflater, parent, true),
                onJoinLivekitRoomClicked = onJoinLivekitRoomClicked,
            )
        }
    }
}
