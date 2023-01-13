package com.nabla.sdk.scheduling.scene

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingAppointmentSummaryViewBinding
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Locale

internal class AppointmentSummaryView(context: Context, attributes: AttributeSet) : FrameLayout(context, attributes) {

    private val binding: NablaSchedulingAppointmentSummaryViewBinding
    private val dateFormatter = SimpleDateFormat(context.getString(R.string.nabla_scheduling_time_format), Locale.getDefault())

    init {
        binding = NablaSchedulingAppointmentSummaryViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun bind(
        locationType: AppointmentLocationType,
        provider: Provider,
        slot: Instant,
        address: Address?,
    ) {
        setAppointmentLocation(locationType)
        setProvider(provider)
        setSlot(slot, locationType)
        setAddress(address)
    }

    private fun setAppointmentLocation(locationType: AppointmentLocationType) {
        val locationIconRes = when (locationType) {
            AppointmentLocationType.PHYSICAL -> R.drawable.nabla_ic_home_20
            AppointmentLocationType.REMOTE -> R.drawable.nabla_ic_video_20
        }

        binding.nablaConfirmAppointmentDate.setCompoundDrawablesRelativeWithIntrinsicBounds(
            locationIconRes,
            0,
            0,
            0
        )
    }

    private fun setProvider(provider: Provider) {
        binding.nablaConfirmAppointmentAvatar.loadAvatar(provider)
        binding.nablaConfirmAppointmentTitle.text = provider.fullNameWithPrefix(binding.context)
        binding.nablaConfirmAppointmentSubtitle.setTextOrHide(provider.title)
    }

    private fun setSlot(slot: Instant, locationType: AppointmentLocationType) {
        binding.nablaConfirmAppointmentDate.text = slot.formatScheduledAt(locationType)
    }

    private fun Instant.formatScheduledAt(locationType: AppointmentLocationType): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isToday = today == toLocalDateTime(TimeZone.currentSystemDefault()).date

        val formattedTime = dateFormatter.format(toJavaDate())

        return if (isToday) {
            val stringRes = when (locationType) {
                AppointmentLocationType.PHYSICAL -> R.string.nabla_scheduling_physical_date_format_today
                AppointmentLocationType.REMOTE -> R.string.nabla_scheduling_remote_date_format_today
            }
            context.getString(stringRes, formattedTime)
        } else {
            val formattedDate = SimpleDateFormat(context.getString(R.string.nabla_scheduling_date_pill_format_date), Locale.getDefault())
                .format(toJavaDate())
            val stringRes = when (locationType) {
                AppointmentLocationType.PHYSICAL -> R.string.nabla_scheduling_physical_date_format_future
                AppointmentLocationType.REMOTE -> R.string.nabla_scheduling_remote_date_format_future
            }
            context.getString(stringRes, formattedDate, formattedTime)
        }
    }

    private fun setAddress(address: Address?) {
        binding.nablaConfirmAppointmentLocation.isVisible = address != null
        binding.nablaConfirmAppointmentLocationExtra.isVisible = address != null
        binding.nablaConfirmAppointmentLocation.text = address?.formatAddress()
        binding.nablaConfirmAppointmentLocationExtra.text = address?.extraDetails
    }

    private fun Address.formatAddress(): String {
        return buildString {
            append("$address, $city $zipCode")
            state?.let { append(", $it") }
            country?.let { append(", $it") }
        }
    }
}
