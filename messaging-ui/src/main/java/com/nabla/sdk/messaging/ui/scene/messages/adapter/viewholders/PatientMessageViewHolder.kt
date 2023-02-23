package com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders

import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.helpers.ColorExtensions.setTextColor
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemPatientMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.content.MessageContentBinder
import com.google.android.material.R as MaterialR

internal sealed class PatientMessageViewHolder<ContentType : TimelineItem.Message.Content, BinderType : MessageContentBinder<ContentType>>(
    private val binding: NablaConversationTimelineItemPatientMessageBinding,
    contentBinder: BinderType,
) : MessageViewHolder<ContentType, TimelineItem.Message.Author.CurrentPatient, BinderType>(contentBinder, binding.root),
    ClickableItemHolder,
    PopUpMenuHolder {
    override val clickableView: View
        get() = binding.root

    override val popUpMenu: PopupMenu = PopupMenu(
        binding.context,
        binding.chatPatientMessageContentContainer,
        Gravity.BOTTOM
    ).apply {
        menuInflater.inflate(R.menu.nabla_message_actions, menu)
    }

    override fun bind(message: TimelineItem.Message, author: TimelineItem.Message.Author.CurrentPatient, content: ContentType) {
        super.bind(message, author, content)
        val showStatus = message.showStatus
        val status = message.status
        bindStatus(status, showStatus)
    }

    fun bindStatus(status: SendStatus, showStatus: Boolean) {
        if (showStatus) {
            binding.chatPatientMessageContentStatusTextView.text = binding.context.getString(
                when (status) {
                    SendStatus.Sending -> R.string.nabla_conversation_message_sending_status
                    SendStatus.Sent -> R.string.nabla_conversation_message_sent_status
                    SendStatus.ErrorSending -> R.string.nabla_conversation_message_error_status
                }
            )
            binding.chatPatientMessageContentStatusTextView.setTextColor(
                binding.context.getThemeColor(
                    when (status) {
                        SendStatus.ErrorSending -> MaterialR.attr.colorError
                        else -> MaterialR.attr.colorOnSurface
                    }
                )
            )
        }
        showStatus(showStatus)
    }

    fun showStatus(showStatus: Boolean) {
        binding.chatPatientMessageContentStatusTextView.isVisible = showStatus
    }
}
