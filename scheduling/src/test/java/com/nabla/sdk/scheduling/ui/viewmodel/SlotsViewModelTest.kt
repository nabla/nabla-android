package com.nabla.sdk.scheduling.ui.viewmodel

import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.stubs.StdLogger
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsUiItem
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsUiItem.DaySlots.ExpansionState
import com.nabla.sdk.scheduling.scene.slots.TimeSlotsViewModel
import com.nabla.sdk.tests.common.BaseCoroutineTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class SlotsViewModelTest : BaseCoroutineTest() {

    @Test
    fun `slots view model handles pagination and user interactions`() = runTest {
        // setup & test data
        val referenceInstant = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.atTime(hour = 8, 0, 0, 0).toInstant(TimeZone.UTC)
        val firstPage = listOf(
            AvailabilitySlot(referenceInstant, uuid4()),
            AvailabilitySlot(referenceInstant + 15.minutes, uuid4()),
            AvailabilitySlot(referenceInstant + 30.minutes, uuid4()),
            AvailabilitySlot(referenceInstant + 1.days, uuid4()),
        )
        val secondPage = listOf(
            AvailabilitySlot(referenceInstant + 1.days + 15.minutes, uuid4()),
            AvailabilitySlot(referenceInstant + 1.days + 30.minutes, uuid4()),
            AvailabilitySlot(referenceInstant + 2.days, uuid4()),
            AvailabilitySlot(referenceInstant + 2.days + 15.minutes, uuid4()),
        )
        val firstExpectedUiModel = listOf(
            TimeSlotsUiItem.DaySlots(
                referenceInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                expansionState = ExpansionState.Expanded(
                    slots = listOf(
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant, false),
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant + 15.minutes, false),
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant + 30.minutes, false),
                    )
                ),
            ),
            TimeSlotsUiItem.DaySlots(
                referenceInstant.plus(1.days).toLocalDateTime(TimeZone.currentSystemDefault()).date,
                expansionState = ExpansionState.Collapsed(slotsCount = 1),
            ),
            TimeSlotsUiItem.Loading,
        )
        val secondExpectedUiModel = listOf(
            TimeSlotsUiItem.DaySlots(
                referenceInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date,
                expansionState = ExpansionState.Expanded(
                    slots = listOf(
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant, false),
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant + 15.minutes, false),
                        TimeSlotsUiItem.DaySlots.Slot(referenceInstant + 30.minutes, false),
                    )
                ),
            ),
            TimeSlotsUiItem.DaySlots(
                referenceInstant.plus(1.days).toLocalDateTime(TimeZone.currentSystemDefault()).date,
                expansionState = ExpansionState.Collapsed(slotsCount = 3),
            ),
            TimeSlotsUiItem.DaySlots(
                referenceInstant.plus(2.days).toLocalDateTime(TimeZone.currentSystemDefault()).date,
                expansionState = ExpansionState.Collapsed(slotsCount = 2),
            ),
        )

        val slotsDataFlow = MutableSharedFlow<WatchPaginatedResponse<List<AvailabilitySlot>>>()
        val viewModel = TimeSlotsViewModel(
            CategoryId(uuid4()),
            object : SchedulingInternalModuleAdapter() {
                override fun watchAvailabilitySlots(categoryId: CategoryId): Flow<WatchPaginatedResponse<List<AvailabilitySlot>>> {
                    return slotsDataFlow
                }
            },
            StdLogger(),
        )
        val firstPageResponse = WatchPaginatedResponse(
            firstPage,
            loadMore = {
                slotsDataFlow.emit(WatchPaginatedResponse(firstPage + secondPage, loadMore = null))
                Result.success(Unit)
            }
        )

        // The actual test scenario
        viewModel.stateFlow.test {
            assertIs<TimeSlotsViewModel.State.Loading>(awaitItem())
            slotsDataFlow.emit(firstPageResponse)

            awaitItem().let { state ->
                assertIs<TimeSlotsViewModel.State.Loaded>(state)
                assertFalse(state.canSubmit)
                assertContentEquals(expected = firstExpectedUiModel, actual = state.items)
            }

            viewModel.onListReachedBottom()

            awaitItem().let { state ->
                assertIs<TimeSlotsViewModel.State.Loaded>(state)
                assertFalse(state.canSubmit)
                assertContentEquals(expected = secondExpectedUiModel, actual = state.items)
            }
            viewModel.onDaySlotsClicked(position = 1)
            awaitItem().let { state ->
                assertIs<TimeSlotsViewModel.State.Loaded>(state)
                assertFalse(state.canSubmit)
                assertIs<ExpansionState.Expanded>((state.items[0] as TimeSlotsUiItem.DaySlots).expansionState)
                assertIs<ExpansionState.Expanded>((state.items[1] as TimeSlotsUiItem.DaySlots).expansionState)
            }
            viewModel.onDaySlotsClicked(position = 0)
            awaitItem().let { state ->
                assertIs<TimeSlotsViewModel.State.Loaded>(state)
                assertFalse(state.canSubmit)
                assertIs<ExpansionState.Collapsed>((state.items[0] as TimeSlotsUiItem.DaySlots).expansionState)
            }

            viewModel.onSlotClicked(referenceInstant + 1.days)
            awaitItem().let { state ->
                assertIs<TimeSlotsViewModel.State.Loaded>(state)
                assertTrue(state.canSubmit)
                assertTrue(((state.items[1] as TimeSlotsUiItem.DaySlots).expansionState as ExpansionState.Expanded).slots[0].isSelected)
            }
        }
    }
}
