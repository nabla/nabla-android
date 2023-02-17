package com.nabla.sdk.scheduling.ui.appointments

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.asStringOrRes
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.scheduling.databinding.NablaSchedulingFragmentAppointmentDetailsBinding
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.scene.details.AppointmentDetailsViewModel
import com.nabla.sdk.scheduling.scene.details.bind
import com.nabla.sdk.scheduling.scene.withNablaSchedulingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class AppointmentDetailsFragmentTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Test loading state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.bind(AppointmentDetailsViewModel.State.Loading(appointment = null))

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loaded state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.bind(AppointmentDetailsViewModel.State.Loaded(appointment = appointment, cancelAvailable = true))

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test loaded state without cancel button`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.bind(AppointmentDetailsViewModel.State.Loaded(appointment = appointment, cancelAvailable = false))

            return@snapshotDayNightDefaultDevice parent
        }
    }

    @Test
    fun `Test error state`() {
        paparazzi.snapshotDayNightDefaultDevice { (context, layoutInflater) ->
            val (parent, binding) = createBinding(context, layoutInflater)

            binding.bind(
                AppointmentDetailsViewModel.State.Error(
                    errorUiModel = ErrorUiModel(
                        title = "Error".asStringOrRes(),
                        body = "Something went wrong".asStringOrRes(),
                    )
                )
            )

            return@snapshotDayNightDefaultDevice parent
        }
    }

    private fun createBinding(context: Context, layoutInflater: LayoutInflater): Pair<View, NablaSchedulingFragmentAppointmentDetailsBinding> {
        val parent = FrameLayout(context.withNablaSchedulingThemeOverlays())
        val binding = NablaSchedulingFragmentAppointmentDetailsBinding.inflate(
            layoutInflater.cloneInContext(context.withNablaSchedulingThemeOverlays()),
            parent,
            false
        )

        parent.addView(binding.root)

        return Pair(parent, binding)
    }

    companion object {
        private val appointment = Appointment(
            id = AppointmentId(UUID.randomUUID()),
            provider = Provider(
                id = Uuid.randomUUID(),
                avatar = null,
                firstName = "Mario",
                lastName = "Bros",
                prefix = "Sir",
                title = "Dr",
            ),
            scheduledAt = Instant.parse("2021-06-01T10:00:00Z"),
            state = AppointmentState.Upcoming,
            location = AppointmentLocation.Physical(
                address = Address(
                    city = "New York",
                    state = "NY",
                    country = "USA",
                    address = "123 Main Street",
                    extraDetails = "Apt 1",
                    zipCode = "12345",
                )
            )
        )
    }
}
