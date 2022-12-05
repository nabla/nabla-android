package com.nabla.sdk.messaging.ui

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays

fun <V : View> LayoutInflater.inflateWithWithNablaMessagingThemeOverlays(@LayoutRes layoutId: Int): V {
    @Suppress("UNCHECKED_CAST")
    return cloneInContext(context.withNablaMessagingThemeOverlays()).inflate(layoutId, null) as V
}
