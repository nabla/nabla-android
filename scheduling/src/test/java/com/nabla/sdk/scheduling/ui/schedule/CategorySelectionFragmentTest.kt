package com.nabla.sdk.scheduling.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.bind
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentCategoriesBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.AppointmentCategoryAdapter
import com.nabla.sdk.scheduling.scene.VerticalOffsetsItemDecoration
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class CategorySelectionFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test loaded state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false
            binding.recyclerView.isVisible = true
            binding.nablaNoCategoryText.isVisible = false

            val adapter = AppointmentCategoryAdapter(
                onClickAppointmentCategoryListener = {},
            )

            binding.recyclerView.adapter = adapter

            adapter.submitList(
                listOf(
                    AppointmentCategory(
                        id = CategoryId(Uuid.randomUUID()),
                        name = "Category 1 with short name",
                        callDuration = 15.minutes,
                    ),
                    AppointmentCategory(
                        id = CategoryId(Uuid.randomUUID()),
                        name = "Category 2 with longer name but not that long",
                        callDuration = 30.minutes,
                    ),
                    AppointmentCategory(
                        id = CategoryId(Uuid.randomUUID()),
                        name = "Category 3 with a much longer name that takes way more than 1 line to be completely displayed even on a large screen !",
                        callDuration = 1.hours,
                    ),
                )
            )

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
            binding.nablaNoCategoryText.isVisible = false

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
            binding.nablaNoCategoryText.isVisible = true

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
            binding.nablaNoCategoryText.isVisible = false

            binding.errorLayout.bind(
                error = ErrorUiModel.Generic,
                onRetryListener = {},
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentCategoriesBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        val binding = NablaSchedulingFragmentCategoriesBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false
        )

        binding.recyclerView.addItemDecoration(VerticalOffsetsItemDecoration())

        parent.addView(binding.root)

        return Pair(parent, binding)
    }
}
