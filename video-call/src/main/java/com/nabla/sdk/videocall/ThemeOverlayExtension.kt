package com.nabla.sdk.videocall

import android.content.Context
import com.nabla.sdk.core.ui.helpers.withNablaThemeOverlay

internal fun Context.withNablaVideoCallThemeOverlays(): Context {
    return this.withNablaThemeOverlay(
        hasValidOverlaysAttr = R.attr.nablaHasValidVideoCallOverlays,
        themeOverlayAttr = R.attr.nablaVideoCallThemeOverlay,
        styleOverlay = null,
        defaultThemeOverlay = R.style.Nabla_ThemeOverlay_VideoCall
    )
}
