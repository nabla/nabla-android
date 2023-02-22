package com.nabla.sdk.scheduling.scene

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.context
import com.nabla.sdk.core.ui.helpers.fullNameWithPrefix
import com.nabla.sdk.core.ui.helpers.setTextOrHide
import com.nabla.sdk.core.ui.helpers.toJavaDate
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.databinding.NablaSchedulingAppointmentSummaryViewBinding
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.Price
import com.nabla.sdk.scheduling.domain.entity.address
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

internal class AppointmentSummaryView(context: Context, attributes: AttributeSet) : FrameLayout(context, attributes) {

    private val binding: NablaSchedulingAppointmentSummaryViewBinding
    private val dateFormatter = SimpleDateFormat(context.getString(R.string.nabla_scheduling_time_format), Locale.getDefault())
    private val currencyFormatter = NumberFormat.getCurrencyInstance()

    init {
        binding = NablaSchedulingAppointmentSummaryViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun bind(
        location: AppointmentLocation,
        provider: Provider,
        slot: Instant,
        price: Price?,
    ) {
        setLocation(location)
        setProvider(provider)
        setSlot(slot)
        setPrice(price)
    }

    private fun setLocation(location: AppointmentLocation) {
        val (locationIconRes, stringRes) = when (location.type) {
            AppointmentLocationType.PHYSICAL -> R.drawable.nabla_ic_home_20 to R.string.nabla_scheduling_location_physical
            AppointmentLocationType.REMOTE -> R.drawable.nabla_ic_video_20 to R.string.nabla_scheduling_location_remote
            null -> 0 to 0
        }

        binding.nablaAppointmentLocationType.setCompoundDrawablesRelativeWithIntrinsicBounds(locationIconRes, 0, 0, 0)
        binding.nablaAppointmentLocationType.setText(stringRes)

        setAddress(location.address)
    }

    private fun setProvider(provider: Provider) {
        binding.nablaAppointmentAvatar.loadAvatar(provider)
        binding.nablaAppointmentTitle.text = provider.fullNameWithPrefix(binding.context)
        binding.nablaAppointmentSubtitle.setTextOrHide(provider.title)
    }

    private fun setSlot(slot: Instant) {
        binding.nablaAppointmentDate.text = slot.formatScheduledAt()
    }

    private fun setPrice(price: Price?) {
        binding.nablaAppointmentPrice.setTextOrHide(price?.formatPrice())
    }

    private fun Instant.formatScheduledAt(): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isToday = today == toLocalDateTime(TimeZone.currentSystemDefault()).date

        val formattedTime = dateFormatter.format(toJavaDate())

        return if (isToday) {
            context.getString(R.string.nabla_scheduling_default_date_format_today, formattedTime)
        } else {
            val formattedDate = SimpleDateFormat(context.getString(R.string.nabla_scheduling_date_pill_format_date), Locale.getDefault())
                .format(toJavaDate())
            context.getString(R.string.nabla_scheduling_default_date_format_future, formattedDate, formattedTime)
        }
    }

    private fun Price.formatPrice(): String {
        currencyFormatter.currency = Currency.getInstance(currencyCode)
        return currencyFormatter.format(amount)
    }

    private fun setAddress(address: Address?) {
        binding.nablaAppointmentAddress.setTextOrHide(address?.formatAddress())
        binding.nablaAppointmentAddressExtra.setTextOrHide(address?.extraDetails)

        if (address != null) {
            binding.nablaAppointmentAddress.setOnClickListener {
                try {
                    val mapsIntentUri = Uri.parse("geo:0,0?q=${address.formatForMapsQuery()}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapsIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    binding.context.startActivity(mapIntent)
                } catch (e: Exception) {
                    // No-op as the app is probably just not installed on the device, so we silence that error
                }
            }
        }
    }

    private fun Address.formatAddress(): String {
        return buildString {
            append("$address, $city $zipCode")
            state?.let { append(", $it") }
            country?.let { append(", $it") }
        }
    }

    private fun Address.formatForMapsQuery(): String {
        val formatted = buildString {
            append("$address, $city $zipCode")
            state?.let { append(", $it") }
            country?.let { append(", $it") }
        }
        return if (Build.VERSION.SDK_INT >= 33) {
            URLEncoder.encode(formatted, Charsets.UTF_8)
        } else {
            @Suppress("DEPRECATION")
            URLEncoder.encode(formatted)
        }
    }
}
