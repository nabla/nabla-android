package com.nabla.sdk.messaging.ui.scene

import android.graphics.Typeface
import android.widget.TextView
import com.nabla.sdk.core.ui.helpers.getThemeColor

// TODO we'll want a better granularity than just bold/notBold, i.e. something like regular/medium/semi/etc.

fun applyConversationListTitleStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTypeface(null, if (hasUnreadMessage) Typeface.BOLD else Typeface.NORMAL)
}

fun applyConversationListSubtitleStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTypeface(null, if (hasUnreadMessage) Typeface.BOLD else Typeface.NORMAL)
    textView.setTextColor(textView.context.getThemeColor(if (hasUnreadMessage) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary))
}

fun applyLastMessageTimeStyle(textView: TextView, hasUnreadMessage: Boolean) {
    textView.setTextColor(textView.context.getThemeColor(if (hasUnreadMessage) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary))
}
