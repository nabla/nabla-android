package com.nabla.sdk.scheduling.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.bind
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentTimeSlotsBinding
import com.nabla.sdk.scheduling.scene.VerticalOffsetsItemDecoration
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsAdapter
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsUiItem
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class TimeSlotsFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test loaded state with disabled continue button`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.progressBar.isVisible = false
            binding.errorLayout.root.isVisible = false
            binding.nablaSlotsContinueButton.isEnabled = false
            binding.nablaNoAvailabilityText.isVisible = false

            val adapter = TimeSlotsAdapter(
                onDaySlotsClicked = { },
                onSlotClicked = { },
                onBindLoading = { },
            )

            binding.recyclerView.adapter = adapter

            adapter.submitList(slots)

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loaded state with enabled continue button`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.progressBar.isVisible = false
            binding.errorLayout.root.isVisible = false
            binding.nablaSlotsContinueButton.isEnabled = true
            binding.nablaNoAvailabilityText.isVisible = false

            val adapter = TimeSlotsAdapter(
                onDaySlotsClicked = { },
                onSlotClicked = { },
                onBindLoading = { },
            )

            binding.recyclerView.adapter = adapter

            adapter.submitList(slots)

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loading state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = true
            binding.nablaSlotsContinueButton.isEnabled = false
            binding.nablaNoAvailabilityText.isVisible = false

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test empty state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = false
            binding.progressBar.isVisible = false
            binding.nablaSlotsContinueButton.isEnabled = true
            binding.nablaNoAvailabilityText.isVisible = true

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test error state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.errorLayout.root.isVisible = true
            binding.progressBar.isVisible = false
            binding.nablaSlotsContinueButton.isEnabled = false
            binding.nablaNoAvailabilityText.isVisible = false

            binding.errorLayout.bind(
                error = ErrorUiModel.Generic,
                onRetryListener = {},
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentTimeSlotsBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        val binding = NablaSchedulingFragmentTimeSlotsBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false,
        )

        binding.recyclerView.addItemDecoration(VerticalOffsetsItemDecoration())

        parent.addView(binding.root)

        return Pair(parent, binding)
    }

    private companion object {
        val distantDate = kotlin.run {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, 2032)
            calendar.set(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 2)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            Instant.fromEpochMilliseconds(calendar.timeInMillis)
        }

        val slots = listOf(
            TimeSlotsUiItem.DaySlots(
                localDate = LocalDate.parse("2029-12-01"),
                expansionState = TimeSlotsUiItem.DaySlots.ExpansionState.Collapsed(slotsCount = 10),
            ),
            TimeSlotsUiItem.DaySlots(
                localDate = LocalDate.parse("2029-12-02"),
                expansionState = TimeSlotsUiItem.DaySlots.ExpansionState.Expanded(
                    slots = listOf(
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = false,
                        ),
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = true,
                        ),
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = false,
                        ),
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = false,
                        ),
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = false,
                        ),
                        TimeSlotsUiItem.DaySlots.Slot(
                            startAt = distantDate,
                            isSelected = false,
                        ),
                    ),
                ),
            ),
            TimeSlotsUiItem.DaySlots(
                localDate = LocalDate.parse("2029-12-03"),
                expansionState = TimeSlotsUiItem.DaySlots.ExpansionState.Collapsed(slotsCount = 1),
            ),
        )
    }
}
