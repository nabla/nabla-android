package com.nabla.sdk.docscanner.ui.extensions

import android.content.Context
import com.nabla.sdk.core.ui.helpers.ThemeOverlayExtension.withNablaThemeOverlay
import com.nabla.sdk.docscanner.R

internal fun Context.withNablaDocScannerThemeOverlays(): Context {
    return this.withNablaThemeOverlay(
        hasValidOverlaysAttr = R.attr.nablaHasValidDocScannerOverlays,
        themeOverlayAttr = R.attr.nablaDocScannerThemeOverlay,
        styleOverlay = null,
        defaultThemeOverlay = R.style.Nabla_ThemeOverlay_DocScanner,
    )
}
