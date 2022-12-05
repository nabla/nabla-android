package com.nabla.sdk.tests.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.NightMode
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

private val defaultSmallDeviceConfig = DeviceConfig.NEXUS_4.copy(locale = "en")
private val defaultDefaultDeviceConfig = DeviceConfig.PIXEL_5.copy(locale = "en")
private val defaultLargeDeviceConfig = DeviceConfig.PIXEL_6_PRO.copy(locale = "en")

private const val theme = "Theme.Material3.DayNight.NoActionBar"

class DayNightPaparazziRule(
    smallDeviceConfig: DeviceConfig = defaultSmallDeviceConfig,
    defaultDeviceConfig: DeviceConfig = defaultDefaultDeviceConfig,
    largerDeviceConfig: DeviceConfig = defaultLargeDeviceConfig,
    renderingMode: SessionParams.RenderingMode = SessionParams.RenderingMode.NORMAL,
) : TestRule {
    private val daySmallDevicePaparazzi = Paparazzi(
        deviceConfig = smallDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NOTNIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    private val nightSmallDevicePaparazzi = Paparazzi(
        deviceConfig = smallDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    private val dayDefaultDevicePaparazzi = Paparazzi(
        deviceConfig = defaultDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NOTNIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    private val nightDefaultDevicePaparazzi = Paparazzi(
        deviceConfig = defaultDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    private val dayLargeDevicePaparazzi = Paparazzi(
        deviceConfig = largerDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NOTNIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    private val nightLargeDevicePaparazzi = Paparazzi(
        deviceConfig = largerDeviceConfig.copy(
            locale = "en",
            nightMode = NightMode.NIGHT,
        ),
        renderingMode = renderingMode,
        theme = theme,
    )

    fun snapshotDayNightDefaultDevice(
        action: (ViewPreparationContext) -> View,
    ) {
        dayDefaultDevicePaparazzi.prepareAndSnapshot(action, "day")
        nightDefaultDevicePaparazzi.prepareAndSnapshot(action, "night")
    }

    fun snapshotDayNightMultiDevices(
        action: (ViewPreparationContext) -> View,
    ) {
        daySmallDevicePaparazzi.prepareAndSnapshot(action, "small_day")
        dayDefaultDevicePaparazzi.prepareAndSnapshot(action, "default_day")
        dayLargeDevicePaparazzi.prepareAndSnapshot(action, "large_day")
        nightSmallDevicePaparazzi.prepareAndSnapshot(action, "small_night")
        nightDefaultDevicePaparazzi.prepareAndSnapshot(action, "default_night")
        nightLargeDevicePaparazzi.prepareAndSnapshot(action, "large_night")
    }

    private fun Paparazzi.prepareAndSnapshot(
        action: (ViewPreparationContext) -> View,
        name: String,
    ) {
        apply(
            object : Statement() {
                override fun evaluate() {
                    snapshot(action(ViewPreparationContext(context, layoutInflater)), name)
                }
            },
            description,
        )
    }

    data class ViewPreparationContext(
        val context: Context,
        val layoutInflater: LayoutInflater,
    )

    private lateinit var description: Description

    override fun apply(base: Statement, description: Description): Statement {
        this.description = description

        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}
