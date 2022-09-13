package com.nabla.sdk.scheduling.scene.appointments

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.nabla.sdk.scheduling.R
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
internal enum class AppointmentType(
    @StringRes val titleRes: Int,
    @StringRes val emptyStateRes: Int,
) : Parcelable {
    UPCOMING(
        R.string.nabla_scheduling_upcoming_appointments,
        R.string.nabla_scheduling_no_upcoming_appointments,
    ),
    PAST(
        R.string.nabla_scheduling_past_appointments,
        R.string.nabla_scheduling_no_past_appointments,
    ),
}
