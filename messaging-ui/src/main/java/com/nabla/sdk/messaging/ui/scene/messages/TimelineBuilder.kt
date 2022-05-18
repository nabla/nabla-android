package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationItem
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.MessageSender
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.core.domain.entity.SendStatus
import kotlinx.datetime.Instant

internal class TimelineBuilder {

    fun buildTimeline(
        items: List<ConversationItem>,
        hasMore: Boolean,
        providersInConversation: List<ProviderInConversation>,
        selectedMessageId: MessageId? = null,
    ): List<TimelineItem> {

        // First generates items containing messages, conversation activity and action request.
        // In this first pass, we don't show the status and the author (will do in a second pass)
        val allMessageItems = items.map { item ->
            when (item) {
                is ConversationActivity -> item.toTimelineItem()
                is Message -> item.toTimelineItem(
                    showSenderAvatar = false,
                    showSenderName = false,
                    showStatus = false,
                )
            }
        }

        // Now that we have the complete list of messages, action request and conversation events,
        // determines if we should show the status and show the author
        val allMessageItemsWithStatus = allMessageItems.mapIndexed { index, item ->
            if (item is TimelineItem.Message) {
                val nextMessage = allMessageItems.getOrNull(index + 1)
                val showSenderAvatarAndName = item.sender != MessageSender.Patient && (
                    nextMessage == null ||
                        nextMessage !is TimelineItem.Message ||
                        nextMessage.sender != item.sender
                    )

                val showStatus = item.status != SendStatus.Sent || item.id == selectedMessageId
                item.copy(showStatus = showStatus, showSenderAvatar = showSenderAvatarAndName, showSenderName = showSenderAvatarAndName)
            } else {
                item
            }
        }

        // If needed inserts dates
        val allItemsWithDates = allMessageItemsWithStatus.flatMapIndexed { index, item ->
            val nextItem = if (index < allMessageItemsWithStatus.size - 1) allMessageItemsWithStatus[index + 1] else null
            val itemDate = item.getDate()
            val shouldShowDate = (nextItem == null && !hasMore) || // First item of timeline
                (item is TimelineItem.Message && selectedMessageId != null && item.id == selectedMessageId) || // Selected message
                shouldShowDate(itemDate, nextItem?.getDate()) // Too much time difference
            if (shouldShowDate && itemDate != null) {
                listOf(item, TimelineItem.DateSeparator(itemDate, "date_of_${item.listItemId}"))
            } else {
                listOf(item)
            }
        }

        val typingIndicators = providersInConversation
            .filter { it.isTyping }
            .mapIndexed { index, typingProvider ->
                val firstMessage = allItemsWithDates.firstOrNull()
                val showProviderName = index > 0 ||
                    firstMessage == null ||
                    (firstMessage is TimelineItem.Message && (firstMessage.sender as? MessageSender.Provider)?.provider != typingProvider.provider) ||
                    firstMessage !is TimelineItem.Message
                TimelineItem.ProviderTypingIndicator(
                    provider = typingProvider.provider,
                    showProviderName = showProviderName
                )
            }

        val loadMoreItems = if (hasMore) listOf(
            TimelineItem.LoadingMore
        ) else emptyList()

        return typingIndicators + allItemsWithDates + loadMoreItems
    }

    private fun shouldShowDate(itemDate: Instant?, previousItemDate: Instant?) =
        itemDate != null &&
            previousItemDate != null &&
            (itemDate - previousItemDate).inWholeHours > 1
}
