package com.nabla.sdk.scheduling.scene

import android.content.Context
import com.nabla.sdk.core.ui.helpers.ThemeOverlayExtension.withNablaThemeOverlay
import com.nabla.sdk.scheduling.R

internal fun Context.withNablaSchedulingThemeOverlays(): Context {
    return this.withNablaThemeOverlay(
        hasValidOverlaysAttr = R.attr.nablaHasValidSchedulingOverlays,
        themeOverlayAttr = R.attr.nablaSchedulingThemeOverlay,
        styleOverlay = null,
        defaultThemeOverlay = R.style.Nabla_ThemeOverlay_Scheduling,
    )
}
