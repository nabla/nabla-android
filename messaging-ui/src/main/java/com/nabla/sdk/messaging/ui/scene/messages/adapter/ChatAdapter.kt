package com.nabla.sdk.messaging.ui.scene.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.recyclerview.widget.ListAdapter
import com.benasher44.uuid.Uuid
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemLoadingMoreBinding
import com.nabla.sdk.messaging.ui.scene.messages.MessageAction
import com.nabla.sdk.messaging.ui.scene.messages.TimelineItem

internal class ChatAdapter(private val callbacks: Callbacks) : ListAdapter<TimelineItem, ChatViewHolder>(ConversationDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TimelineItem.Message -> {
                when (item.content) {
                    is TimelineItem.Message.Text -> {
                        when (item.sender) {
                            is MessageSender.Patient -> ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            is MessageSender.Provider -> ViewType.PROVIDER_TEXT_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_TEXT_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Image -> {
                        when (item.sender) {
                            is MessageSender.Patient -> ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            is MessageSender.Provider -> ViewType.PROVIDER_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_IMAGE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.File -> {
                        when (item.sender) {
                            is MessageSender.Patient -> ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE.ordinal
                            is MessageSender.Provider -> ViewType.PROVIDER_FILE_MESSAGE_VIEW_TYPE.ordinal
                            else -> ViewType.SYSTEM_FILE_MESSAGE_VIEW_TYPE.ordinal
                        }
                    }
                    is TimelineItem.Message.Deleted -> {
                        if (item.sender is MessageSender.Patient) {
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
            ViewType.PROVIDER_TYPING_INDICATOR -> ProviderTypingIndicatorViewHolder.create(inflater, parent)
            ViewType.PATIENT_DELETED_MESSAGE_VIEW_TYPE -> PatientDeletedMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_TEXT_MESSAGE_VIEW_TYPE -> PatientTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = true) },
            )
            ViewType.PATIENT_FILE_MESSAGE_VIEW_TYPE -> PatientFileMessageViewHolder.create(inflater, parent)
            ViewType.PATIENT_IMAGE_MESSAGE_VIEW_TYPE -> PatientImageMessageViewHolder.create(inflater, parent)
            ViewType.SYSTEM_TEXT_MESSAGE_VIEW_TYPE -> SystemTextMessageViewHolder.create(
                inflater,
                parent,
                onUrlClicked = { callbacks.onUrlClicked(it, isFromPatient = false) },
            )
            ViewType.SYSTEM_FILE_MESSAGE_VIEW_TYPE -> SystemFileMessageViewHolder.create(inflater, parent)
            ViewType.SYSTEM_IMAGE_MESSAGE_VIEW_TYPE -> SystemImageMessageViewHolder.create(inflater, parent)
            ViewType.LOADING_MORE_VIEW_TYPE -> LoadingMoreViewHolder(NablaConversationTimelineItemLoadingMoreBinding.inflate(inflater, parent, false))
            ViewType.DATE_VIEW_TYPE -> DateSeparatorViewHolder.create(inflater, parent)
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
                (holder as? ProviderImageMessageViewHolder ?: holder as? PatientImageMessageViewHolder)?.contentBinder
                    ?.loadImage(
                        uri = payload.uri,
                        itemId = payload.itemId,
                    )

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
                item.sender as MessageSender.Provider,
                item.content as TimelineItem.Message.Deleted
            )
            is ProviderFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Provider,
                item.content as TimelineItem.Message.File
            )
            is ProviderImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Provider,
                item.content as TimelineItem.Message.Image
            )
            is ProviderTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Provider,
                item.content as TimelineItem.Message.Text
            )
            is PatientDeletedMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Patient,
                item.content as TimelineItem.Message.Deleted
            )
            is PatientFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Patient,
                item.content as TimelineItem.Message.File
            )
            is PatientImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Patient,
                item.content as TimelineItem.Message.Image
            )
            is PatientTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.Patient,
                item.content as TimelineItem.Message.Text
            )
            is SystemTextMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.System,
                item.content as TimelineItem.Message.Text
            )
            is SystemFileMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.System,
                item.content as TimelineItem.Message.File
            )
            is SystemImageMessageViewHolder -> holder.bind(
                item as TimelineItem.Message,
                item.sender as MessageSender.System,
                item.content as TimelineItem.Message.Image
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
    }

    private enum class ViewType {
        DATE_VIEW_TYPE,
        LOADING_MORE_VIEW_TYPE,
        PROVIDER_DELETED_MESSAGE_VIEW_TYPE,
        PROVIDER_FILE_MESSAGE_VIEW_TYPE,
        PROVIDER_IMAGE_MESSAGE_VIEW_TYPE,
        PROVIDER_TEXT_MESSAGE_VIEW_TYPE,
        PROVIDER_TYPING_INDICATOR,
        PATIENT_DELETED_MESSAGE_VIEW_TYPE,
        PATIENT_FILE_MESSAGE_VIEW_TYPE,
        PATIENT_IMAGE_MESSAGE_VIEW_TYPE,
        PATIENT_TEXT_MESSAGE_VIEW_TYPE,
        SYSTEM_FILE_MESSAGE_VIEW_TYPE,
        SYSTEM_IMAGE_MESSAGE_VIEW_TYPE,
        SYSTEM_TEXT_MESSAGE_VIEW_TYPE,
    }
}
