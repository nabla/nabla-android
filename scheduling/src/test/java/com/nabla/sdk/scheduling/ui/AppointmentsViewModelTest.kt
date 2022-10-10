package com.nabla.sdk.scheduling.ui

import app.cash.turbine.test
import com.nabla.sdk.core.data.stubs.StdLogger
import com.nabla.sdk.core.data.stubs.TestClock
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.scheduling.core.data.stubs.fake
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.scene.appointments.AppointmentType
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsContentViewModel
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus
import com.nabla.sdk.scheduling.scene.appointments.SOON_CONVERSATION_THRESHOLD
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.FakeStringResolver
import com.nabla.sdk.tests.common.FakeVideoCallModule
import com.nabla.sdk.tests.common.extensions.collectToFlow
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

class AppointmentsViewModelTest : BaseCoroutineTest() {

    @Test
    fun `upcoming appointments change to SoonOrOngoing when imminent`() = runTest {
        // setup & test data
        val now = TestClock(this).now()
        val appointmentsResponse = WatchPaginatedResponse(
            content = listOf<Appointment>(
                Appointment.Upcoming.fake(scheduledAt = now + SOON_CONVERSATION_THRESHOLD + 2.minutes),
                Appointment.Upcoming.fake(scheduledAt = now + SOON_CONVERSATION_THRESHOLD + 3.minutes),
                Appointment.Upcoming.fake(scheduledAt = now + SOON_CONVERSATION_THRESHOLD + 30.minutes),
            ),
            loadMore = null,
        )
        val appointmentDataFlow = MutableSharedFlow<WatchPaginatedResponse<List<Appointment>>>()
        val schedulingClient = object : SchedulingInternalModuleAdapter() {
            override fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> = appointmentDataFlow
        }
        val viewModel = AppointmentsContentViewModel(
            schedulingClient,
            videoCallModule = FakeVideoCallModule(),
            logger = StdLogger(),
            configuration = mockk(relaxed = true),
            stringResolver = FakeStringResolver(),
            clock = TestClock(this),
            delayCoroutineContext = coroutineContext,
            appointmentType = AppointmentType.UPCOMING,
        )

        // The actual test scenario
        viewModel.stateFlow.test {
            assertIs<AppointmentsContentViewModel.State.Loading>(awaitItem())
            appointmentDataFlow.emit(appointmentsResponse)

            awaitItem().let { state ->
                assertIs<AppointmentsContentViewModel.State.Loaded>(state)
                val nextAppointment = state.items[0]
                assertIs<AppointmentUiModel.Upcoming>(nextAppointment)
            }

            // two minutes later the first appointment is now SoonOrOngoing
            awaitItem().let { state ->
                assertIs<AppointmentsContentViewModel.State.Loaded>(state)
                val (nextAppointment, secondAppointment, farAppointment) = state.items
                assertIs<AppointmentUiModel.SoonOrOngoing>(nextAppointment)
                assertIs<AppointmentUiModel.Upcoming>(secondAppointment)
                assertIs<AppointmentUiModel.Upcoming>(farAppointment)
                assertIs<CallButtonStatus.Present.AsJoin>(nextAppointment.callButtonStatus)
            }

            // one minutes later the second appointment is now SoonOrOngoing too
            awaitItem().let { state ->
                assertIs<AppointmentsContentViewModel.State.Loaded>(state)
                val (nextAppointment, secondAppointment, farAppointment) = state.items
                assertIs<AppointmentUiModel.SoonOrOngoing>(nextAppointment)
                assertIs<AppointmentUiModel.SoonOrOngoing>(secondAppointment)
                assertIs<AppointmentUiModel.Upcoming>(farAppointment)
                assertIs<CallButtonStatus.Present.AsJoin>(nextAppointment.callButtonStatus)
                assertIs<CallButtonStatus.Present.AsJoin>(secondAppointment.callButtonStatus)

                viewModel.onJoinClicked(
                    nextAppointment.callButtonStatus.videoCallRoom,
                    nextAppointment.callButtonStatus.videoCallRoom.status as VideoCallRoomStatus.Open
                )
            }

            // joining a call disables all buttons except the "go back" one
            awaitItem().let { state ->
                assertIs<AppointmentsContentViewModel.State.Loaded>(state)
                val (nextAppointment, secondAppointment, _) = state.items
                assertIs<AppointmentUiModel.SoonOrOngoing>(nextAppointment)
                assertIs<AppointmentUiModel.SoonOrOngoing>(secondAppointment)
                assertIs<CallButtonStatus.Present.AsGoBack>(nextAppointment.callButtonStatus)
                assertIs<CallButtonStatus.Absent>(secondAppointment.callButtonStatus)
            }
        }
    }

    @Test
    fun `view model is catching errors from domain layer`() = runTest {
        val schedulingClient = object : SchedulingInternalModuleAdapter() {
            var firstCall = AtomicBoolean(true)
            override fun watchUpcomingAppointments(): Flow<WatchPaginatedResponse<List<Appointment>>> =
                flowOf(WatchPaginatedResponse(listOf(Appointment.Upcoming.fake() as Appointment)) { Result.failure(Exception()) })
                    .onStart {
                        if (firstCall.compareAndSet(true, false)) error("simulated error")
                    }
        }
        val clock = TestClock(this)
        val viewModel = AppointmentsContentViewModel(
            schedulingClient = schedulingClient,
            videoCallModule = FakeVideoCallModule(),
            logger = StdLogger(),
            configuration = mockk(relaxed = true),
            stringResolver = FakeStringResolver(),
            clock = clock,
            delayCoroutineContext = coroutineContext,
            appointmentType = AppointmentType.UPCOMING,
        )

        // State error management
        viewModel.stateFlow.test {
            assertIs<AppointmentsContentViewModel.State.Error>(awaitItem())
            viewModel.onRetryClicked()
            assertIs<AppointmentsContentViewModel.State.Loading>(awaitItem())
            assertIs<AppointmentsContentViewModel.State.Loaded>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val eventSharedFlow = MutableSharedFlow<AppointmentsContentViewModel.Event>()
        val collector = launch(coroutineContext) { viewModel.eventsFlow.collectToFlow(eventSharedFlow) }

        // Events error management
        eventSharedFlow.test {
            viewModel.onListReachedBottom()
            assertIs<AppointmentsContentViewModel.Event.FailedPagination>(awaitItem())

            viewModel.onCancelClicked(
                (viewModel.stateFlow.value as AppointmentsContentViewModel.State.Loaded)
                    .items.first() as AppointmentUiModel.Upcoming
            )
            assertIs<AppointmentsContentViewModel.Event.FailedCancelling>(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
        collector.cancel()
    }
}
