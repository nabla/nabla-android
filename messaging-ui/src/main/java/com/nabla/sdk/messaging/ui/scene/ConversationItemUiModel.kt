package com.nabla.sdk.messaging.ui.scene

import android.content.Context
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.User
import com.nabla.sdk.core.ui.helpers.isThisWeek
import com.nabla.sdk.core.ui.helpers.isThisYear
import com.nabla.sdk.core.ui.helpers.isToday
import com.nabla.sdk.core.ui.helpers.toFormattedDayOfMonth
import com.nabla.sdk.core.ui.helpers.toFormattedNumericDate
import com.nabla.sdk.core.ui.helpers.toFormattedShortWeekDay
import com.nabla.sdk.core.ui.helpers.toFormattedTime
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import java.util.Date

data class ConversationItemUiModel(
    val id: Id,
    val title: String,
    val subtitle: String,
    val lastModified: Date,
    val hasUnreadMessages: Boolean,
    val providers: List<User.Provider>,
) {
    fun formatLastModified(context: Context): String {
        return when {
            lastModified.isToday() -> lastModified.toFormattedTime(context)
            lastModified.isThisWeek() -> lastModified.toFormattedShortWeekDay(context)
            lastModified.isThisYear() -> lastModified.toFormattedDayOfMonth(context)
            else -> lastModified.toFormattedNumericDate(context)
        }
    }
}

fun Conversation.toUiModel() = ConversationItemUiModel(
    id = id,
    title = inboxPreviewTitle,
    subtitle = inboxPreviewSubtitle,
    lastModified = Date(lastModified.toEpochMilliseconds()),
    hasUnreadMessages = patientUnreadMessageCount > 0,
    providers = providers,
)
