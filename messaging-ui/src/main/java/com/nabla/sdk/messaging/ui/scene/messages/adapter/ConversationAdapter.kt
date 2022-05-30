package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.ListAdapter
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageAuthor
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemLoadingMoreBinding
import com.nabla.sdk.messaging.ui.scene.messages.MessageAction
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ChatViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ClickableItemHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ConversationActivityTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.DateSeparatorViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.LoadingMoreViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientDeletedMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PatientTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.PopUpMenuHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderDeletedMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderTextMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.ProviderTypingIndicatorViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.SystemAudioMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.SystemFileMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.SystemImageMessageViewHolder
import com.nabla.sdk.messaging.ui.scene.messages.adapter.viewholders.SystemTextMessageViewHolder

internal class ConversationAdapter(private val callbacks: Callbacks) : ListAdapter<TimelineItem, ChatViewHolder>(ConversationDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineItem.Message -> {
                when (item.content) {
                    is TimelineItem.Message.Text -> {
                        when (item.author) {
                            is MessageAuthor.Patient -> ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            is MessageAuthor.Provider -> ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_TEXT_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Image -> {
                        when (item.author) {
                            is MessageAuthor.Patient -> ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            is MessageAuthor.Provider -> ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.File -> {
                        when (item.author) {
                            is MessageAuthor.Patient -> ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE.ordinal
                            is MessageAuthor.Provider -> ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_FILE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Audio -> {
                        when (item.author) {
                            is MessageAuthor.Patient -> ViewType.PATIENT_AUDIO_MESSAGE_VIEW_TYPE.ordinal
                            is MessageAuthor.Provider -> ViewType.PROVIDER_AUDIO_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_FILE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Deleted -> {
                        if (item.author is MessageAuthor.Patient) {
                            ViewType.PATIENT_DELETED_MESSAGE_VIEW_TYPE.ordinal
                        } else {
                            ViewType.PROVIDER_DELETED_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
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
            ViewType.PROVIDER_DELETED_MESSAGE_VIEW_TYPE -> ProviderDeletedMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE -> ProviderTextMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = false) },
            )
            ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE -> ProviderFileMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE -> ProviderImageMessageViewHolder.create(inflater, parent, callbacks::onProviderClicked)
            ViewType.PROVIDER_AUDIO_MESSAGE_VIEW_TYPE -> ProviderAudioMessageViewHolder.create(
                inflater,
                parent,
                callbacks::onProviderClicked,
                callbacks::onToggleAudioMessagePlay
            )
            ViewType.PROVIDER_TYPING_INDICATOR -> ProviderTypingIndicatorViewHolder.create(inflater, parent)
            ViewType.PATIENT_DELETED_MESSAGE_VIEW_TYPE -> PatientDeletedMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE -> PatientTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = true) },
            )
            ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE -> PatientFileMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE -> PatientImageMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_AUDIO_MESSAGE_VIEW_TYPE -> PatientAudioMessageViewHolder.create(inflater, parent, callbacks::onToggleAudioMessagePlay)
            ViewType.SYSTEM_TEXT_MESSAGE_VIEW_TYPE -> SystemTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = false) },
            )
            ViewType.SYSTEM_FILE_MESSAGE_VIEW_TYPE -> SystemFileMessageViewHolder.create(inflater, parent)
            ViewType.SYSTEM_IMAGE_MESSAGE_VIEW_TYPE -> SystemImageMessageViewHolder.create(inflater, parent)
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
                (holder as? ProviderImageMessageViewHolder ?: holder as? PatientImageMessageViewHolder ?: holder as? SystemImageMessageViewHolder)
                    ?.contentBinder
                    ?.loadImage(
                        uri = payload.uri,
                        itemId = payload.itemId,
                    )

                (holder as? PatientImageMessageViewHolder)?.bindStatus(payload.status, payload.showStatus)
            }
            is BindingPayload.Audio -> {
                (holder as? ProviderAudioMessageViewHolder ?: holder as? PatientAudioMessageViewHolder ?: holder as? SystemAudioMessageViewHolder)
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
                item.author as MessageAuthor.Provider,
                item.content as TimelineItem.Message.Deleted
            )
            is ProviderFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Provider,
                item.content as TimelineItem.Message.File
            )
            is ProviderImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Provider,
                item.content as TimelineItem.Message.Image
            )
            is ProviderAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Provider,
                item.content as TimelineItem.Message.Audio,
            )
            is ProviderTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Provider,
                item.content as TimelineItem.Message.Text
            )
            is PatientDeletedMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Patient,
                item.content as TimelineItem.Message.Deleted
            )
            is PatientFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Patient,
                item.content as TimelineItem.Message.File
            )
            is PatientImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Patient,
                item.content as TimelineItem.Message.Image
            )
            is PatientAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Patient,
                item.content as TimelineItem.Message.Audio,
            )
            is PatientTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.Patient,
                item.content as TimelineItem.Message.Text
            )
            is SystemTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.System,
                item.content as TimelineItem.Message.Text
            )
            is SystemFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.System,
                item.content as TimelineItem.Message.File
            )
            is SystemImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.System,
                item.content as TimelineItem.Message.Image
            )
            is SystemAudioMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.author as MessageAuthor.System,
                item.content as TimelineItem.Message.Audio,
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
                    R.id.messageActionCopy -> menuItem.isVisible = actions.contains(MessageAction.Copy)
                    R.id.messageActionDelete -> menuItem.isVisible = actions.contains(MessageAction.Delete)
                }
            }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
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
        fun onProviderClicked(providerId: Uuid)
        fun onUrlClicked(url: String, isFromPatient: Boolean)
        fun onToggleAudioMessagePlay(audioMessageUri: Uri)
    }

    private enum class ViewType {
        DATE_VIEW_TYPE,
        LOADING_MORE_VIEW_TYPE,
        PROVIDER_DELETED_MESSAGE_VIEW_TYPE,
        PROVIDER_FILE_MESSAGE_VIEW_TYPE,
        PROVIDER_IMAGE_MESSAGE_VIEW_TYPE,
        PROVIDER_AUDIO_MESSAGE_VIEW_TYPE,
        PROVIDER_TEXT_MESSAGE_VIEW_TYPE,
        PROVIDER_TYPING_INDICATOR,
        PATIENT_DELETED_MESSAGE_VIEW_TYPE,
        PATIENT_FILE_MESSAGE_VIEW_TYPE,
        PATIENT_IMAGE_MESSAGE_VIEW_TYPE,
        PATIENT_AUDIO_MESSAGE_VIEW_TYPE,
        PATIENT_TEXT_MESSAGE_VIEW_TYPE,
        SYSTEM_FILE_MESSAGE_VIEW_TYPE,
        SYSTEM_IMAGE_MESSAGE_VIEW_TYPE,
        SYSTEM_TEXT_MESSAGE_VIEW_TYPE,
        CONVERSATION_ACTIVITY_TEXT_MESSAGE_VIEW_TYPE,
    }
}
