package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.text.Spannable
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.text.getSpans
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.getThemeStyle
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemTextMessageBinding
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class TextMessageContentBinder(
    @AttrRes contentAppearanceAttr: Int,
    private val binding: NablaConversationTimelineItemTextMessageBinding,
    private val onUrlClicked: (url: String) -> Unit,
) : MessageContentBinder<TimelineItem.Message.Text>() {

    private val contentAppearance: Int = binding.context.getThemeStyle(contentAppearanceAttr)

    override fun bind(messageId: String, item: TimelineItem.Message.Text) {
        bindTextAndSpans(item)
        binding.chatTextMessageTextView.setTextAppearance(contentAppearance)
    }

    private fun bindTextAndSpans(item: TimelineItem.Message.Text) {
        with(binding.chatTextMessageTextView) {
            if (text.toString() == item.text) return

            autoLinkMask = Linkify.ALL
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

    companion object {
        fun create(
            @AttrRes contentAppearanceAttr: Int,
            inflater: LayoutInflater,
            parent: ViewGroup,
            onUrlClicked: (url: String) -> Unit,
        ): TextMessageContentBinder {
            return TextMessageContentBinder(
                contentAppearanceAttr = contentAppearanceAttr,
                binding = NablaConversationTimelineItemTextMessageBinding.inflate(inflater, parent, true),
                onUrlClicked,
            )
        }
    }
}
