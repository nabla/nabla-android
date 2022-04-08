package com.nabla.sdk.core.ui.helpers

import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Remove the callback to be invoked when this view is clicked. If this view is
 * clickable, it becomes not clickable.
 *
 * @see View.setOnClickListener
 */
fun View.removeOnClickListener() {
    setOnClickListener(null)
    isClickable = false
}

fun View.setOnClickLabel(@StringRes stringRes: Int, vararg args: Any) {
    ViewCompat.setAccessibilityDelegate(
        this,
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        host.context.getString(stringRes, *args)
                    )
                )
            }
        }
    )
}
