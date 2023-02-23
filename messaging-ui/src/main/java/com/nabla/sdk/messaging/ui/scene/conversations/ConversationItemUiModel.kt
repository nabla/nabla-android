package com.nabla.sdk.messaging.ui.scene.conversations

import android.content.Context
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.DateFormattingExtension.toFormattedDayOfMonth
import com.nabla.sdk.core.ui.helpers.DateFormattingExtension.toFormattedNumericDate
import com.nabla.sdk.core.ui.helpers.DateFormattingExtension.toFormattedShortWeekDay
import com.nabla.sdk.core.ui.helpers.DateFormattingExtension.toFormattedTime
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.isThisWeek
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.isThisYear
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.isToday
import com.nabla.sdk.core.ui.helpers.DateParsingExtension.toJavaDate
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.datetime.Instant

internal sealed class ItemUiModel(val listId: String) {

    object Loading : ItemUiModel(listId = "loading_more")

    data class Conversation(
        val id: ConversationId,
        val title: StringOrRes,
        val subtitle: String,
        val lastModified: Instant,
        val hasUnreadMessages: Boolean,
        val providers: List<Provider>,
        val pictureUrl: Uri?,
    ) : ItemUiModel(listId = id.stableId.toString()) {

        fun formatLastModified(context: Context): String {
            val date = lastModified.toJavaDate()
            return when {
                date.isToday() -> date.toFormattedTime(context)
                date.isThisWeek() -> date.toFormattedShortWeekDay(context)
                date.isThisYear() -> date.toFormattedDayOfMonth(context)
                else -> date.toFormattedNumericDate(context)
            }
        }
    }
}

internal fun Conversation.toUiModel() = ItemUiModel.Conversation(
    id = id,
    title = inboxPreviewTitle,
    subtitle = lastMessagePreview ?: subtitle ?: "",
    lastModified = lastModified,
    hasUnreadMessages = patientUnreadMessageCount > 0,
    providers = providersInConversation.map { it.provider },
    pictureUrl = pictureUrl?.url,
)
