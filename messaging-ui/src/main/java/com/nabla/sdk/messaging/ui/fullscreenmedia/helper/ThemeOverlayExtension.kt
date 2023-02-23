package com.nabla.sdk.messaging.ui.fullscreenmedia.helper

import android.content.Context
import android.util.AttributeSet
import com.nabla.sdk.core.ui.helpers.StyleOverlay
import com.nabla.sdk.core.ui.helpers.ThemeOverlayExtension.withNablaThemeOverlay
import com.nabla.sdk.messaging.ui.R

internal fun Context.withNablaMessagingThemeOverlays(attrs: AttributeSet? = null): Context {
    return this.withNablaThemeOverlay(
        hasValidOverlaysAttr = R.attr.nablaHasValidMessagingOverlays,
        themeOverlayAttr = R.attr.nablaMessagingThemeOverlay,
        styleOverlay = attrs?.let { StyleOverlay(attrs, R.styleable.NablaThemeOverlayApplier) },
        defaultThemeOverlay = R.style.Nabla_ThemeOverlay_Messaging,
    )
}
