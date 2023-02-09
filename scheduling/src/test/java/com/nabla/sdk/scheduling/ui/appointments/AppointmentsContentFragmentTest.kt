package com.nabla.sdk.scheduling.ui.appointments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentsContentBinding
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.scene.appointments.AppointmentsAdapter
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

class AppointmentsContentFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Appointments list test`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            val adapter = AppointmentsAdapter(
                onJoinClicked = { _, _ -> },
                onJoinExternalClicked = { },
                onDetailsClicked = { },
            )

            adapter.overrideAvatarBackgroundRandomSeed(null)

            binding.nablaAppointmentsRecyclerView.adapter = adapter

            binding.nablaAppointmentsRecyclerView.isVisible = true
            binding.nablaIncludedErrorLayout.root.isVisible = false
            binding.nablaLoadingProgressBar.isVisible = false
            binding.nablaNoAppointmentText.isVisible = false

            adapter.submitList(
                listOf(
                    ItemUiModel.AppointmentUiModel.Upcoming(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Instant.DISTANT_FUTURE,
                    ),
                    ItemUiModel.AppointmentUiModel.SoonOrOngoing(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Clock.System.now().plus(1.minutes),
                        callButtonStatus = ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.Absent,
                    ),
                    ItemUiModel.AppointmentUiModel.SoonOrOngoing(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Clock.System.now().minus(5.minutes),
                        callButtonStatus = ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.Absent,
                    ),
                    ItemUiModel.AppointmentUiModel.SoonOrOngoing(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Clock.System.now().plus(5.minutes),
                        callButtonStatus = ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.Absent,
                    ),
                    ItemUiModel.AppointmentUiModel.SoonOrOngoing(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Clock.System.now().plus(5.minutes),
                        callButtonStatus = ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.ForVideoCall.AsJoin(videoCallRoom),
                    ),
                    ItemUiModel.AppointmentUiModel.SoonOrOngoing(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Clock.System.now().plus(5.minutes),
                        callButtonStatus = ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.ForVideoCall.AsGoBack(videoCallRoom),
                    ),
                    ItemUiModel.AppointmentUiModel.Finalized(
                        id = AppointmentId(Uuid.randomUUID()),
                        provider = provider,
                        scheduledAt = Instant.DISTANT_PAST,
                    ),
                )
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentAppointmentsContentBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        parent.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, context.dpToPx(800))
        val binding = NablaSchedulingFragmentAppointmentsContentBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false
        )

        parent.addView(binding.root)

        return Pair(parent, binding)
    }

    private companion object {
        val provider = Provider(
            id = Uuid.randomUUID(),
            avatar = null,
            firstName = "Mario",
            lastName = "Bros",
            prefix = "Sir",
            title = "Dr",
        )
        val videoCallRoom = VideoCallRoom(
            id = Uuid.randomUUID(),
            status = VideoCallRoomStatus.Open(
                url = "https://www.google.com",
                token = "token",
            )
        )
    }
}
