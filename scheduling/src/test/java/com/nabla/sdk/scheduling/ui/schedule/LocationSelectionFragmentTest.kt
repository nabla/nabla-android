package com.nabla.sdk.scheduling.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentLocationsBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.scene.AppointmentLocationAdapter
import com.nabla.sdk.scheduling.scene.VerticalOffsetsItemDecoration
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import org.junit.Rule
import org.junit.Test

class LocationSelectionFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test loaded state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false
            binding.recyclerView.isVisible = true
            binding.nablaNoLocationText.isVisible = false

            val adapter = AppointmentLocationAdapter(
                onClickAppointmentLocationTypeListener = {},
            )

            binding.recyclerView.adapter = adapter
            adapter.submitList(AppointmentLocationType.values().toList())

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loading state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = true
            binding.recyclerView.isVisible = false
            binding.nablaNoLocationText.isVisible = false

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test empty state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false
            binding.recyclerView.isVisible = false
            binding.nablaNoLocationText.isVisible = true

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test error state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = true
            binding.progressBar.isVisible = false
            binding.recyclerView.isVisible = false
            binding.nablaNoLocationText.isVisible = false

            binding.errorLayout.bind(
                error = ErrorUiModel.Generic,
                onRetryListener = {},
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentLocationsBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        val binding = NablaSchedulingFragmentLocationsBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false
        )

        binding.recyclerView.addItemDecoration(VerticalOffsetsItemDecoration())

        parent.addView(binding.root)

        return Pair(parent, binding)
    }
}
