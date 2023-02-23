package com.nabla.sdk.messaging.ui.scene.conversations

import android.graphics.Typeface
import android.widget.TextView
import com.nabla.sdk.core.ui.helpers.ColorExtensions.setTextColor
import com.nabla.sdk.core.ui.helpers.ThemeExtension.getThemeColor

// TODO we'll want a better granularity than just bold/notBold, i.e. something like regular/medium/semi/etc.

internal fun applyConversationListTitleStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTypeface(textView.typeface, if (hasUnreadMessage) Typeface.BOLD else Typeface.NORMAL)
}

internal fun applyConversationListSubtitleStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTypeface(textView.typeface, if (hasUnreadMessage) Typeface.BOLD else Typeface.NORMAL)
    textView.setTextColor(textView.context.getThemeColor(if (hasUnreadMessage) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary))
}

internal fun applyLastMessageTimeStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTextColor(textView.context.getThemeColor(if (hasUnreadMessage) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary))
}
