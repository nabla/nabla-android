package com.nabla.sdk.tests.common

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams

fun defaultPaparazzi(
    renderingMode: SessionParams.RenderingMode = SessionParams.RenderingMode.NORMAL,
) = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5.copy(
        locale = "en",
    ),
    theme = "Theme.Material3.DayNight.NoActionBar",
    renderingMode = renderingMode
)
