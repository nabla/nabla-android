package com.nabla.sdk.messaging.ui

import android.view.View
import androidx.annotation.LayoutRes
import app.cash.paparazzi.Paparazzi
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays

fun <V : View> Paparazzi.inflateWithWithNablaMessagingThemeOverlays(@LayoutRes layoutId: Int): V {
    @Suppress("UNCHECKED_CAST")
    return layoutInflater.cloneInContext(context.withNablaMessagingThemeOverlays()).inflate(layoutId, null) as V
}
