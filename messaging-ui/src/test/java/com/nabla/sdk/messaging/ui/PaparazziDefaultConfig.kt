package com.nabla.sdk.messaging.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams

internal fun defaultPaparazzi(
    renderingMode: SessionParams.RenderingMode = SessionParams.RenderingMode.NORMAL
) = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    theme = "Theme.Material3.DayNight.NoActionBar",
    renderingMode = renderingMode
)
