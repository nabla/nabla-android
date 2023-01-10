package com.nabla.sdk.core.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import app.cash.paparazzi.DeviceConfig
import com.nabla.sdk.core.R
import com.nabla.sdk.core.ui.components.NablaAvatarView
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import org.junit.Rule
import org.junit.Test

internal class NablaAvatarViewTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule(
        defaultDeviceConfig = DeviceConfig(
            screenHeight = 250,
            screenWidth = 200,
            softButtons = false,
        )
    )

    @Test
    fun `Default placeholder avatar`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView()

            avatarView.loadAvatar(avatarUrl = null, placeholderText = null)

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Avatar view initials 2 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView()

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "BL")

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Avatar view initials 1 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView()

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "B")

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Squared Default placeholder avatar`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_Squared)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = null)

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Squared Avatar view initials 2 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_Squared)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "BL")

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Squared Avatar view initials 1 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_Squared)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "B")

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Round Rect Default placeholder avatar`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_RoundRect)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = null)

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Round Rect Avatar view initials 2 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_RoundRect)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "BL")

            return@snapshotDayNightDefaultDevice container
        }
    }

    @Test
    fun `Round Rect Avatar view initials 1 chars`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, _) ->
            val (container, avatarView) = context.makeNablaAvatarView(R.style.NablaTest_ThemeOverlay_AvatarView_RoundRect)

            avatarView.loadAvatar(avatarUrl = null, placeholderText = "B")

            return@snapshotDayNightDefaultDevice container
        }
    }

    private fun Context.makeNablaAvatarView(themeOverlay: Int? = null): Pair<View, NablaAvatarView> {
        val avatarView = NablaAvatarView(themeOverlay?.let { ContextThemeWrapper(this, it) } ?: this)

        val container = FrameLayout(this)
        container.layoutParams = FrameLayout.LayoutParams(200, 200)

        container.addView(avatarView)

        return Pair(container, avatarView)
    }
}
