package com.nabla.sdk.messaging.ui.scene.conversations

import android.content.Context
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.core.ui.helpers.isThisWeek
import com.nabla.sdk.core.ui.helpers.isThisYear
import com.nabla.sdk.core.ui.helpers.isToday
import com.nabla.sdk.core.ui.helpers.toFormattedDayOfMonth
import com.nabla.sdk.core.ui.helpers.toFormattedNumericDate
import com.nabla.sdk.core.ui.helpers.toFormattedShortWeekDay
import com.nabla.sdk.core.ui.helpers.toFormattedTime
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.datetime.Instant

internal sealed class ItemUiModel(val listId: String) {

    object Loading : ItemUiModel("loading")

    data class Conversation(
        val id: ConversationId,
        val title: String,
        val subtitle: String,
        val lastModified: Instant,
        val hasUnreadMessages: Boolean,
        val providers: List<User.Provider>,
    ) : ItemUiModel(listId = id.value.toString()) {

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
    title = title ?: "",
    subtitle = description ?: "",
    lastModified = lastModified,
    hasUnreadMessages = patientUnreadMessageCount > 0,
    providers = providersInConversation.map { it.provider },
)
