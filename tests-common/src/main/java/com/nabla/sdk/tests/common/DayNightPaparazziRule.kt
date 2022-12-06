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
        offsetMillis: Long = 0L,
        action: (ViewPreparationContext) -> View,
    ) {
        dayDefaultDevicePaparazzi.prepareAndSnapshot(action, "day", offsetMillis)
        nightDefaultDevicePaparazzi.prepareAndSnapshot(action, "night", offsetMillis)
    }

    fun snapshotDayNightMultiDevices(
        offsetMillis: Long = 0L,
        action: (ViewPreparationContext) -> View,
    ) {
        daySmallDevicePaparazzi.prepareAndSnapshot(action, "small_day", offsetMillis)
        dayDefaultDevicePaparazzi.prepareAndSnapshot(action, "default_day", offsetMillis)
        dayLargeDevicePaparazzi.prepareAndSnapshot(action, "large_day", offsetMillis)
        nightSmallDevicePaparazzi.prepareAndSnapshot(action, "small_night", offsetMillis)
        nightDefaultDevicePaparazzi.prepareAndSnapshot(action, "default_night", offsetMillis)
        nightLargeDevicePaparazzi.prepareAndSnapshot(action, "large_night", offsetMillis)
    }

    private fun Paparazzi.prepareAndSnapshot(
        action: (ViewPreparationContext) -> View,
        name: String,
        offsetMillis: Long = 0L,
    ) {
        apply(
            object : Statement() {
                override fun evaluate() {
                    snapshot(
                        view = action(ViewPreparationContext(context, layoutInflater)),
                        name = name,
                        offsetMillis = offsetMillis,
                    )
                }
            },
            description,
        ).evaluate()
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
