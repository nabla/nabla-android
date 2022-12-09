package com.nabla.sdk.scheduling.scene.appointments

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.core.ui.helpers.capitalize
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingAppointmentItemBinding
import com.nabla.sdk.scheduling.databinding.NablaSchedulingAppointmentItemLoadingMoreBinding
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel.Finalized
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel.SoonOrOngoing
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus.Present
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

internal class LoadingMoreViewHolder(val binding: NablaSchedulingAppointmentItemLoadingMoreBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(parent: ViewGroup): LoadingMoreViewHolder {
            return LoadingMoreViewHolder(
                NablaSchedulingAppointmentItemLoadingMoreBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }
}

internal class AppointmentViewHolder(
    private val binding: NablaSchedulingAppointmentItemBinding,
    private val onCancelClicked: (appointment: AppointmentUiModel.Upcoming) -> Unit,
    private val onJoinClicked: (room: VideoCallRoom, roomStatus: VideoCallRoomStatus.Open) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val context
        get() = binding.context

    private var timeUpdateJob: Job? = null

    private var avatarRandomSeedOverridden = false
    private var overriddenAvatarRandomSeed: Any? = null

    @VisibleForTesting
    fun overrideAvatarBackgroundRandomSeed(seed: Any?) {
        avatarRandomSeedOverridden = true
        overriddenAvatarRandomSeed = seed
    }

    fun bind(uiModel: AppointmentUiModel) {
        if (avatarRandomSeedOverridden) {
            binding.appointmentAvatar.loadAvatar(uiModel.provider, overriddenAvatarRandomSeed)
        } else {
            binding.appointmentAvatar.loadAvatar(uiModel.provider)
        }
        binding.appointmentTitle.text = uiModel.provider.fullNameWithPrefix(context)
        binding.appointmentSubtitle.text = uiModel.formatScheduledAt()

        scheduleTimeUpdates(uiModel)

        binding.optionsButton.isVisible = uiModel is AppointmentUiModel.Upcoming
        binding.joinCallButton.setTextOrHide(
            if (uiModel is SoonOrOngoing && uiModel.callButtonStatus is Present) {
                binding.context.getString(
                    when (uiModel.callButtonStatus) {
                        is Present.AsJoin -> R.string.nabla_scheduling_appointment_item_join_cta
                        is Present.AsGoBack -> R.string.nabla_scheduling_appointment_item_return_cta
                    }
                )
            } else null
        )
        val alpha = if (uiModel is Finalized) .50f else 1f
        binding.appointmentTitle.alpha = alpha
        binding.appointmentSubtitle.alpha = alpha

        when (uiModel) {
            is Finalized -> Unit /* no-op */
            is SoonOrOngoing -> {
                if (uiModel.callButtonStatus is Present) {
                    val status = uiModel.callButtonStatus.videoCallRoom.status
                    if (status is VideoCallRoomStatus.Open) {
                        binding.joinCallButton.setOnClickListener {
                            onJoinClicked(uiModel.callButtonStatus.videoCallRoom, status)
                        }
                    }
                }
            }
            is AppointmentUiModel.Upcoming -> setupOptionsMenu(uiModel)
        }
    }

    private fun scheduleTimeUpdates(appointment: AppointmentUiModel) {
        binding.root.doOnLayout {
            timeUpdateJob?.cancel()
            if (appointment is Finalized) return@doOnLayout

            timeUpdateJob = it.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                while (isActive) {
                    binding.appointmentSubtitle.text = appointment.formatScheduledAt()
                    delay(remainingSecondsInCurrentMinute())
                }
            }
        }
    }

    private fun setupOptionsMenu(upcomingAppointment: AppointmentUiModel.Upcoming) {
        binding.optionsButton.setOnClickListener {
            PopupMenu(context, binding.root, Gravity.END).apply {
                menuInflater.inflate(R.menu.nabla_appointment_item_actions, menu)

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.cancelAppointment -> {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(context.getString(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_title))
                                .setNegativeButton(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_negative) { _, _ -> /* no-op */ }
                                .setPositiveButton(R.string.nabla_scheduling_appointment_item_cancel_confirmation_dialog_positive) { _, _ ->
                                    onCancelClicked(upcomingAppointment)
                                }
                                .create()
                                .show()

                            true
                        }
                        else -> false
                    }
                }

                show()
            }
        }
    }

    private val dateFormatter =
        SimpleDateFormat(context.getString(R.string.nabla_scheduling_appointment_item_full_date_pattern), Locale.getDefault())

    private fun AppointmentUiModel.formatScheduledAt(): String {
        return when (this) {
            is SoonOrOngoing -> {
                val minutesFromNow = scheduledAt.minus(Clock.System.now()).inWholeMinutes.toInt()

                if (abs(minutesFromNow) <= 59) {
                    context.resources.getQuantityString(
                        if (minutesFromNow >= 0) {
                            R.plurals.nabla_scheduling_appointment_item_relative_future_minutes_pattern
                        } else R.plurals.nabla_scheduling_appointment_item_relative_past_minutes_pattern,
                        minutesFromNow.absoluteValue,
                        minutesFromNow.absoluteValue,
                    )
                } else {
                    dateFormatter.format(scheduledAt.toJavaDate()).capitalize()
                }
            }
            is AppointmentUiModel.Upcoming,
            is Finalized,
            -> {
                dateFormatter.format(scheduledAt.toJavaDate()).capitalize()
            }
        }
    }

    companion object {
        // e.g. 23s if it's 12:34:37 and 1min if it's 12:34:00
        private fun remainingSecondsInCurrentMinute() =
            ceil((60_000 - (Clock.System.now().toEpochMilliseconds() % 60_000)) / 1_000f).toInt().seconds

        fun create(
            parent: ViewGroup,
            onCancelClicked: (appointment: AppointmentUiModel.Upcoming) -> Unit,
            onJoinClicked: (room: VideoCallRoom, roomStatus: VideoCallRoomStatus.Open) -> Unit,
        ): AppointmentViewHolder {
            val binding = NablaSchedulingAppointmentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AppointmentViewHolder(binding, onCancelClicked, onJoinClicked)
        }
    }
}
