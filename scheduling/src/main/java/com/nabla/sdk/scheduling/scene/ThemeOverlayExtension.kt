package com.nabla.sdk.scheduling.scene

import android.content.Context
import com.nabla.sdk.core.ui.helpers.withNablaThemeOverlay
import com.nabla.sdk.scheduling.R

internal fun Context.withNablaVideoCallThemeOverlays(): Context {
    return this.withNablaThemeOverlay(
        hasValidOverlaysAttr = R.attr.nablaHasValidSchedulingOverlays,
        themeOverlayAttr = R.attr.nablaSchedulingThemeOverlay,
        styleOverlay = null,
        defaultThemeOverlay = R.style.Nabla_ThemeOverlay_Scheduling
    )
}
