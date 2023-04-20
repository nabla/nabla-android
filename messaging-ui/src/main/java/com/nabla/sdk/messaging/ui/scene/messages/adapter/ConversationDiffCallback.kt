package com.nabla.sdk.messaging.ui.scene.messages.adapter

import androidx.recyclerview.widget.DiffUtil
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal object ConversationDiffCallback : DiffUtil.ItemCallback<TimelineItem>() {
    override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
        return oldItem.listItemId == newItem.listItemId
    }

    override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
        return when {
            oldItem is TimelineItem.DateSeparator && newItem is TimelineItem.DateSeparator -> true
            else -> oldItem == newItem
        }
    }

    override fun getChangePayload(oldItem: TimelineItem, newItem: TimelineItem): Any? {
        return when {
            oldItem is TimelineItem.Message && newItem is TimelineItem.Message -> {
                getMessageChangePayload(newItem, oldItem)
            }
            else -> {
                null
            }
        }
    }

    private fun getMessageChangePayload(
        newItem: TimelineItem.Message,
        oldItem: TimelineItem.Message,
    ): BindingPayload? {
        val onlyCallbacksChanged = newItem == oldItem.copy(
            id = newItem.id,
            actions = newItem.actions,
            time = newItem.time, // date change is ignored
        )
        val sentByPatient = newItem.author is TimelineItem.Message.Author.CurrentPatient
        val onlyStatusChanged = newItem == oldItem.copy(
            id = newItem.id,
            status = newItem.status,
            showStatus = newItem.showStatus,
            actions = newItem.actions,
            time = newItem.time, // date change is ignored
        )
        return when {
            onlyCallbacksChanged -> BindingPayload.Callbacks(newItem.actions, newItem)
            sentByPatient && onlyStatusChanged -> BindingPayload.PatientSendStatus(
                status = newItem.status,
                showStatus = newItem.showStatus,
                actions = newItem.actions,
                itemForCallback = newItem,
            )
            oldItem.content is TimelineItem.Message.Audio && newItem.content is TimelineItem.Message.Audio -> getAudioMessageChangePayload(
                newItem,
                newItem.content,
                oldItem.content,
            )
            oldItem.content is TimelineItem.Message.Image && newItem.content is TimelineItem.Message.Image -> getImageMessageChangePayload(
                newItem,
                newItem.content,
                oldItem.content,
            )
            oldItem.content is TimelineItem.Message.Video && newItem.content is TimelineItem.Message.Video -> getVideoMessageChangePayload(
                newItem,
                newItem.content,
                oldItem.content,
            )
            else -> null
        }
    }

    private fun getAudioMessageChangePayload(
        newItem: TimelineItem.Message,
        newContent: TimelineItem.Message.Audio,
        oldContent: TimelineItem.Message.Audio,
    ): BindingPayload.Audio? {
        val onlyUriOrProgressChanged = newContent == oldContent.copy(
            uri = newContent.uri,
            progress = newContent.progress,
            isPlaying = newContent.isPlaying,
        )

        return when {
            onlyUriOrProgressChanged -> {
                BindingPayload.Audio(
                    uri = newContent.uri,
                    isPlaying = newContent.isPlaying,
                    progress = newContent.progress,
                    status = newItem.status,
                    showStatus = newItem.showStatus,
                    actions = newItem.actions,
                    itemForCallback = newItem,
                )
            }
            else -> null
        }
    }

    private fun getImageMessageChangePayload(
        newItem: TimelineItem.Message,
        newContent: TimelineItem.Message.Image,
        oldContent: TimelineItem.Message.Image,
    ): BindingPayload.Image? {
        val onlyImageUriChanged = newContent == oldContent.copy(uri = newContent.uri)
        return when {
            onlyImageUriChanged -> BindingPayload.Image(
                itemId = newItem.listItemId,
                uri = newContent.uri.toAndroidUri(),
                showStatus = newItem.showStatus,
                status = newItem.status,
                actions = newItem.actions,
                itemForCallback = newItem,
            )
            else -> null
        }
    }

    private fun getVideoMessageChangePayload(
        newItem: TimelineItem.Message,
        newContent: TimelineItem.Message.Video,
        oldContent: TimelineItem.Message.Video,
    ): BindingPayload.Video? {
        val onlyVideoUriChanged = newContent == oldContent.copy(uri = newContent.uri)
        return when {
            onlyVideoUriChanged -> BindingPayload.Video(
                itemId = newItem.listItemId,
                uri = newContent.uri.toAndroidUri(),
                showStatus = newItem.showStatus,
                status = newItem.status,
                actions = newItem.actions,
                itemForCallback = newItem,
            )
            else -> null
        }
    }
}
