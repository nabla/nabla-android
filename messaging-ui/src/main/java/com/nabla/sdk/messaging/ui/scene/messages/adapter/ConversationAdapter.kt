package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemLoadingMoreBinding
import com.nabla.sdk.messaging.ui.scene.messages.MessageAction
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ChatViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ClickableItemHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ConversationActivityTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.DateSeparatorViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.LoadingMoreViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherDeletedMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.OtherVideoMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientDeletedMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientVideoMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PopUpMenuHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderDeletedMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderLivekitRoomMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderTypingIndicatorViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderVideoMessageViewHolder

internal class ConversationAdapter(private val callbacks: Callbacks) : ListAdapter<TimelineItem, ChatViewHolder>(ConversationDiffCallback) {

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        val swipeToReplyCallback = SwipeToReplyCallback(
            context = recyclerView.context,
            swipeToReplyItemViewTypes = listOf(
                ViewType.PROVIDER_AUDIO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PROVIDER_VIDEO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE.ordinal,

                ViewType.PATIENT_AUDIO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PATIENT_VIDEO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE.ordinal,

                ViewType.OTHER_AUDIO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.OTHER_FILE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.OTHER_IMAGE_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.OTHER_VIDEO_MESSAGE_VIEW_TYPE.ordinal,
                ViewType.OTHER_TEXT_MESSAGE_VIEW_TYPE.ordinal,
            )
        ) { viewHolder ->
            val item = getItem(viewHolder.bindingAdapterPosition)
            if (item is TimelineItem.Message && item.status == SendStatus.Sent) {
                callbacks.onReplyToMessage(item)
            }
        }
        ItemTouchHelper(swipeToReplyCallback).attachToRecyclerView(recyclerView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineItem.Message -> {
                when (item.content) {
                    is TimelineItem.Message.Text -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_TEXT_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Image -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Video -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_VIDEO_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_VIDEO_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_VIDEO_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.File -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_FILE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Audio -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_AUDIO_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_AUDIO_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_AUDIO_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Deleted -> {
                        when (item.author) {
                            is TimelineItem.Message.Author.CurrentPatient -> ViewType.PATIENT_DELETED_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Provider -> ViewType.PROVIDER_DELETED_MESSAGE_VIEW_TYPE.ordinal
                            is TimelineItem.Message.Author.Other -> ViewType.OTHER_DELETED_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.LivekitRoom -> ViewType.PROVIDER_LIVEKIT_OPEN_ROOM_INTERACTIVE_MESSAGE_VIEW_TYPE.ordinal
                }
            }
            is TimelineItem.LoadingMore -> ViewType.LOADING_MORE_VIEW_TYPE.ordinal
            is TimelineItem.DateSeparator -> ViewType.DATE_VIEW_TYPE.ordinal
            is TimelineItem.ProviderTypingIndicator -> ViewType.PROVIDER_TYPING_INDICATOR.ordinal
            is TimelineItem.ConversationActivity -> ViewType.CONVERSATION_ACTIVITY_TEXT_MESSAGE_VIEW_TYPE.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (ViewType.values()[viewType]) {
            ViewType.PROVIDER_LIVEKIT_OPEN_ROOM_INTERACTIVE_MESSAGE_VIEW_TYPE -> ProviderLivekitRoomMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                callbacks::onJoinLivekitRoomClicked
            )
            ViewType.PROVIDER_DELETED_MESSAGE_VIEW_TYPE -> ProviderDeletedMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE -> ProviderFileMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE -> ProviderImageMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_VIDEO_MESSAGE_VIEW_TYPE -> ProviderVideoMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                callbacks::onErrorFetchingVideoThumbnail,
            )
            ViewType.PROVIDER_AUDIO_MESSAGE_VIEW_TYPE -> ProviderAudioMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                callbacks::onToggleAudioMessagePlay
            )
            ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE -> ProviderTextMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = false) },
                onRepliedMessageClicked = callbacks::onRepliedMessageClicked,
            )
            ViewType.PROVIDER_TYPING_INDICATOR -> ProviderTypingIndicatorViewHolder.create(inflater, parent)

            ViewType.PATIENT_DELETED_MESSAGE_VIEW_TYPE -> PatientDeletedMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE -> PatientFileMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE -> PatientImageMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_VIDEO_MESSAGE_VIEW_TYPE -> PatientVideoMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onErrorFetchingVideoThumbnail,
            )
            ViewType.PATIENT_AUDIO_MESSAGE_VIEW_TYPE -> PatientAudioMessageViewHolder.create(inflater, parent, callbacks::onToggleAudioMessagePlay)
            ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE -> PatientTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = true) },
                onRepliedMessageClicked = callbacks::onRepliedMessageClicked,
            )

            ViewType.OTHER_DELETED_MESSAGE_VIEW_TYPE -> OtherDeletedMessageViewHolder.create(inflater, parent)
            ViewType.OTHER_FILE_MESSAGE_VIEW_TYPE -> OtherFileMessageViewHolder.create(inflater, parent)
            ViewType.OTHER_IMAGE_MESSAGE_VIEW_TYPE -> OtherImageMessageViewHolder.create(inflater, parent)
            ViewType.OTHER_VIDEO_MESSAGE_VIEW_TYPE -> OtherVideoMessageViewHolder.create(inflater, parent, callbacks::onErrorFetchingVideoThumbnail)
            ViewType.OTHER_AUDIO_MESSAGE_VIEW_TYPE -> OtherAudioMessageViewHolder.create(inflater, parent, callbacks::onToggleAudioMessagePlay)
            ViewType.OTHER_TEXT_MESSAGE_VIEW_TYPE -> OtherTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = false) },
                onRepliedMessageClicked = callbacks::onRepliedMessageClicked,
            )

            ViewType.LOADING_MORE_VIEW_TYPE -> LoadingMoreViewHolder(NablaConversationTimelineItemLoadingMoreBinding.inflate(inflater, parent, false))
            ViewType.DATE_VIEW_TYPE -> DateSeparatorViewHolder.create(inflater, parent)
            ViewType.CONVERSATION_ACTIVITY_TEXT_MESSAGE_VIEW_TYPE -> ConversationActivityTextMessageViewHolder.create(inflater, parent)
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { applyPayload(holder, it as BindingPayload) }
        }
    }

    private fun applyPayload(holder: ChatViewHolder, payload: BindingPayload) {
        if (holder is ClickableItemHolder) {
            holder.bindOnItemClicked(payload.itemForCallback)
        }
        if (holder is PopUpMenuHolder && payload is BindingPayload.ResetActions) {
            holder.bindLongPressPopupMenu(payload.actions, payload.itemForCallback)
        }
        if (holder is PatientMessageViewHolder<*, *> && payload is BindingPayload.StatusVisibility) {
            holder.showStatus(payload.showStatus)
        }
        when (payload) {
            is BindingPayload.PatientSendStatus -> (holder as PatientMessageViewHolder<*, *>).bindStatus(payload.status, payload.showStatus)
            is BindingPayload.Image -> {
                (holder as? ProviderImageMessageViewHolder ?: holder as? PatientImageMessageViewHolder ?: holder as? OtherImageMessageViewHolder)
                    ?.contentBinder
                    ?.loadImage(
                        uri = payload.uri,
                        itemId = payload.itemId,
                    )

                (holder as? PatientImageMessageViewHolder)?.bindStatus(payload.status, payload.showStatus)
            }
            is BindingPayload.Video -> {
                (holder as? ProviderVideoMessageViewHolder ?: holder as? PatientVideoMessageViewHolder ?: holder as? OtherVideoMessageViewHolder)
                    ?.contentBinder
                    ?.loadVideo(
                        uri = payload.uri,
                        itemId = payload.itemId,
                    )

                (holder as? PatientImageMessageViewHolder)?.bindStatus(payload.status, payload.showStatus)
            }
            is BindingPayload.Audio -> {
                (holder as? ProviderAudioMessageViewHolder ?: holder as? PatientAudioMessageViewHolder ?: holder as? OtherAudioMessageViewHolder)
                    ?.contentBinder
                    ?.bind(payload.uri, payload.isPlaying, payload.progress)

                (holder as? PatientImageMessageViewHolder)?.bindStatus(payload.status, payload.showStatus)
            }
            is BindingPayload.Callbacks -> Unit // callbacks already reset
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is DateSeparatorViewHolder -> holder.bind(item as TimelineItem.DateSeparator)
            is ProviderTypingIndicatorViewHolder -> holder.bind(item as TimelineItem.ProviderTypingIndicator)
            is LoadingMoreViewHolder -> Unit /* no-op */
            is ProviderDeletedMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.Deleted
            )
            is ProviderFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.File
            )
            is ProviderImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.Image
            )
            is ProviderVideoMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.Video
            )
            is ProviderAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.Audio,
            )
            is ProviderTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.Text
            )
            is ProviderLivekitRoomMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Provider,
                item.content as TimelineItem.Message.LivekitRoom
            )
            is PatientDeletedMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.Deleted
            )
            is PatientFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.File
            )
            is PatientImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.Image
            )
            is PatientVideoMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.Video
            )
            is PatientAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.Audio,
            )
            is PatientTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.CurrentPatient,
                item.content as TimelineItem.Message.Text
            )
            is OtherTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.Text
            )
            is OtherFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.File
            )
            is OtherImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.Image
            )
            is OtherVideoMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.Video
            )
            is OtherAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.Audio,
            )
            is OtherDeletedMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as TimelineItem.Message.Author.Other,
                item.content as TimelineItem.Message.Deleted,
            )
            is ConversationActivityTextMessageViewHolder -> holder.bind(
                item as TimelineItem.ConversationActivity
            )
        }

        if (holder is ClickableItemHolder) {
            holder.bindOnItemClicked(item)
        }
        if (holder is PopUpMenuHolder && item is TimelineItem.Message) {
            holder.bindLongPressPopupMenu(item.actions, itemForCallback = item)
        }
    }

    private fun ClickableItemHolder.bindOnItemClicked(item: TimelineItem) {
        clickableView.setOnClickListener {
            callbacks.onItemClicked(item)
        }
    }

    private fun PopUpMenuHolder.bindLongPressPopupMenu(actions: Set<MessageAction>, itemForCallback: TimelineItem.Message) {
        if (actions.isEmpty()) return

        val popup = popUpMenu.apply {
            menu.forEach { menuItem ->
                when (menuItem.itemId) {
                    R.id.messageActionReply -> menuItem.isVisible = actions.contains(MessageAction.Reply)
                    R.id.messageActionCopy -> menuItem.isVisible = actions.contains(MessageAction.Copy)
                    R.id.messageActionDelete -> menuItem.isVisible = actions.contains(MessageAction.Delete)
                }
            }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.messageActionReply -> callbacks.onReplyToMessage(itemForCallback)
                    R.id.messageActionCopy -> (itemForCallback.content as? TimelineItem.Message.Text)?.let(callbacks::onCopyMessage)
                    R.id.messageActionDelete -> callbacks.onDeleteMessage(itemForCallback)
                }
                true
            }
        }
        clickableView.setOnLongClickListener {
            popup.show()
            true
        }
    }

    interface Callbacks {
        fun onItemClicked(item: TimelineItem)
        fun onDeleteMessage(item: TimelineItem.Message)
        fun onCopyMessage(item: TimelineItem.Message.Text)
        fun onReplyToMessage(item: TimelineItem.Message)
        fun onProviderClicked(providerId: Uuid)
        fun onUrlClicked(url: String, isFromPatient: Boolean)
        fun onToggleAudioMessagePlay(audioMessageUri: Uri)
        fun onRepliedMessageClicked(messageId: MessageId)
        fun onErrorFetchingVideoThumbnail(error: Throwable)
        fun onJoinLivekitRoomClicked(url: String, roomId: String, accessToken: String)
    }

    private enum class ViewType {
        DATE_VIEW_TYPE,
        LOADING_MORE_VIEW_TYPE,
        CONVERSATION_ACTIVITY_TEXT_MESSAGE_VIEW_TYPE,

        PROVIDER_LIVEKIT_OPEN_ROOM_INTERACTIVE_MESSAGE_VIEW_TYPE,
        PROVIDER_DELETED_MESSAGE_VIEW_TYPE,
        PROVIDER_FILE_MESSAGE_VIEW_TYPE,
        PROVIDER_IMAGE_MESSAGE_VIEW_TYPE,
        PROVIDER_VIDEO_MESSAGE_VIEW_TYPE,
        PROVIDER_AUDIO_MESSAGE_VIEW_TYPE,
        PROVIDER_TEXT_MESSAGE_VIEW_TYPE,
        PROVIDER_TYPING_INDICATOR,

        PATIENT_DELETED_MESSAGE_VIEW_TYPE,
        PATIENT_FILE_MESSAGE_VIEW_TYPE,
        PATIENT_IMAGE_MESSAGE_VIEW_TYPE,
        PATIENT_VIDEO_MESSAGE_VIEW_TYPE,
        PATIENT_AUDIO_MESSAGE_VIEW_TYPE,
        PATIENT_TEXT_MESSAGE_VIEW_TYPE,

        OTHER_DELETED_MESSAGE_VIEW_TYPE,
        OTHER_FILE_MESSAGE_VIEW_TYPE,
        OTHER_IMAGE_MESSAGE_VIEW_TYPE,
        OTHER_VIDEO_MESSAGE_VIEW_TYPE,
        OTHER_AUDIO_MESSAGE_VIEW_TYPE,
        OTHER_TEXT_MESSAGE_VIEW_TYPE,
    }
}
