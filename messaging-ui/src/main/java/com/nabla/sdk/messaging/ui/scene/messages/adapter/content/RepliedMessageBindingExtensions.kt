package com.nabla.sdk.messaging.ui.scene.messages.adapter.content

import android.content.Context
import android.widget.ImageView
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.ui.helpers.abbreviatedNameWithPrefix
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.scene.messages.RepliedMessage

internal fun RepliedMessage.repliedToContent(context: Context): String = when (content) {
    is RepliedMessage.Content.Text -> content.text
    is RepliedMessage.Content.Deleted -> context.getString(R.string.nabla_conversation_replied_to_message_description_deleted)
    is RepliedMessage.Content.Audio -> context.getString(R.string.nabla_conversation_replied_to_message_description_voice)
    is RepliedMessage.Content.Document -> context.getString(R.string.nabla_conversation_replied_to_message_description_document)
    is RepliedMessage.Content.Image -> context.getString(R.string.nabla_conversation_replied_to_message_description_photo)
    is RepliedMessage.Content.Video -> context.getString(R.string.nabla_conversation_replied_to_message_description_video)
    RepliedMessage.Content.LivekitRoom -> context.getString(R.string.nabla_conversation_video_consultation)
}

internal fun RepliedMessage.repliedToAuthorName(context: Context): String = when (author) {
    is MessageAuthor.Provider -> author.provider.abbreviatedNameWithPrefix(context)
    is MessageAuthor.System -> author.system.name
    is MessageAuthor.Patient -> context.getString(R.string.nabla_conversation_replied_to_message_author_patient)
    is MessageAuthor.DeletedProvider, MessageAuthor.Unknown -> ""
}

internal fun ImageView.loadReplyContentThumbnailOrHide(replyContent: RepliedMessage.Content) {
    isVisible = when {
        replyContent is RepliedMessage.Content.Image -> {
            load(replyContent.uri.toAndroidUri())
            true
        }
        replyContent is RepliedMessage.Content.Video -> {
            load(replyContent.uri.toAndroidUri())
            true
        }
        replyContent is RepliedMessage.Content.Document && replyContent.thumbnailUri != null -> {
            load(replyContent.thumbnailUri.toAndroidUri())
            true
        }
        else -> {
            dispose()
            false
        }
    }
}
