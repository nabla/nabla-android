package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.getSpans
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.resources.TextAppearance
import com.nabla.sdk.core.ui.helpers.TextViewExtension.setTextOrHide
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeStyle
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.context
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemRepliedMessageViewBinding
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemTextMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.RepliedMessage
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

@SuppressLint("RestrictedApi") // TextAppearance class
internal class TextMessageContentBinder(
    @AttrRes contentAppearanceAttr: Int,
    private val binding: NablaConversationTimelineItemTextMessageBinding,
    private val onUrlClicked: (url: String) -> Unit,
    private val onRepliedMessageClicked: (MessageId) -> Unit,
) : MessageContentBinder<TimelineItem.Message.Text>() {

    private val contentAppearance = TextAppearance(binding.context, binding.context.getThemeStyle(contentAppearanceAttr))
    private val contentAppearanceRes: Int = binding.context.getThemeStyle(contentAppearanceAttr)

    override fun bind(messageId: String, item: TimelineItem.Message.Text) {
        bindTextAndSpans(item)
        binding.chatTextMessageTextView.setTextAppearance(contentAppearanceRes)

        binding.repliedMessage.bindReply(item.repliedMessage)
    }

    private fun bindTextAndSpans(item: TimelineItem.Message.Text) {
        with(binding.chatTextMessageTextView) {
            if (text.toString() == item.text) return

            autoLinkMask = Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS or Linkify.WEB_URLS
            text = item.text // give it text so it processes & adds url spans
            autoLinkMask = 0

            // replace them with custom
            val spanned = (text as? Spannable)?.apply {
                getSpans<URLSpan>().forEach { urlSpan ->
                    setSpan(
                        object : URLSpan(urlSpan.url) {
                            override fun onClick(widget: View) {
                                onUrlClicked(url)
                            }
                        },
                        getSpanStart(urlSpan),
                        getSpanEnd(urlSpan),
                        getSpanFlags(urlSpan),
                    )
                    removeSpan(urlSpan)
                }
            }

            if (spanned != null) {
                text = spanned
            }
        }
    }

    private fun NablaConversationTimelineItemRepliedMessageViewBinding.bindReply(repliedMessage: RepliedMessage?) {
        root.isVisible = repliedMessage != null

        if (repliedMessage == null) {
            return
        }

        repliedToTextView.setTextColor(contentAppearance.textColor)
        authorRepliedToTextView.setTextColor(contentAppearance.textColor)
        repliedToIndentView.backgroundTintList = contentAppearance.textColor

        authorRepliedToTextView.setTextOrHide(repliedMessage.repliedToAuthorName(context))

        repliedToTextView.text = repliedMessage.repliedToContent(context)
        repliedToImagePreview.loadReplyContentThumbnailOrHide(repliedMessage.content)
        // we don't want strings like "Voice message" to be split
        val isText = repliedMessage.content is RepliedMessage.Content.Text
        repliedToTextView.maxLines = if (isText) 2 else 1
        repliedToTextView.updateLayoutParams<ConstraintLayout.LayoutParams> { constrainedWidth = isText }

        root.setOnClickListener { onRepliedMessageClicked(repliedMessage.id) }
    }

    companion object {
        fun create(
            @AttrRes contentAppearanceAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
            onUrlClicked: (url: String) -> Unit,
            onRepliedMessageClicked: (MessageId) -> Unit,
        ): TextMessageContentBinder {
            return TextMessageContentBinder(
                contentAppearanceAttr = contentAppearanceAttr,
                binding = NablaConversationTimelineItemTextMessageBinding.inflate(inflater, parent, true),
                onUrlClicked,
                onRepliedMessageClicked,
            )
        }
    }
}
