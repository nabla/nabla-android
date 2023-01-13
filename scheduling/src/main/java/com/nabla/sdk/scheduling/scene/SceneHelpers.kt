package com.nabla.sdk.scheduling.scene

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.ui.helpers.getSerializableCompat
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import java.util.UUID

internal fun Fragment.setAppointmentCategoryId(categoryId: AppointmentCategoryId) {
    val bundle = arguments ?: Bundle()
    bundle.putString(ARG_APPOINTMENT_CATEGORY_ID, categoryId.value.toString())
    arguments = bundle
}

internal fun Fragment.requireAppointmentCategoryId(): AppointmentCategoryId {
    return arguments?.getString(ARG_APPOINTMENT_CATEGORY_ID)?.let { AppointmentCategoryId(UUID.fromString(it)) }
        ?: throwNablaInternalException("Missing category id")
}

private const val ARG_APPOINTMENT_CATEGORY_ID = "ARG_APPOINTMENT_CATEGORY_ID"

internal fun Fragment.setAppointmentLocationType(locationType: AppointmentLocationType) {
    val bundle = arguments ?: Bundle()
    bundle.putSerializable(ARG_APPOINTMENT_LOCATION_TYPE, locationType)
    arguments = bundle
}

internal fun Fragment.requireAppointmentLocationType(): AppointmentLocationType {
    return arguments?.getSerializableCompat(ARG_APPOINTMENT_LOCATION_TYPE, AppointmentLocationType::class.java)
        ?: throwNablaInternalException("Missing appointment location")
}

private const val ARG_APPOINTMENT_LOCATION_TYPE = "ARG_APPOINTMENT_LOCATION_TYPE"

internal fun Intent.setAppointmentId(appointmentId: AppointmentId) {
    putExtra(ARG_APPOINTMENT_ID, appointmentId.uuid.toString())
}

internal fun Intent.requireAppointmentId(): AppointmentId {
    return getStringExtra(ARG_APPOINTMENT_ID)?.let { AppointmentId(UUID.fromString(it)) }
        ?: throwNablaInternalException("Missing appointment id")
}

internal fun Fragment.setAppointmentId(appointmentId: AppointmentId) {
    val bundle = arguments ?: Bundle()
    bundle.putString(ARG_APPOINTMENT_ID, appointmentId.uuid.toString())
    arguments = bundle
}

internal fun Fragment.requireAppointmentId(): AppointmentId {
    return arguments?.getString(ARG_APPOINTMENT_ID)?.let { AppointmentId(UUID.fromString(it)) }
        ?: throwNablaInternalException("Missing appointment id")
}

private const val ARG_APPOINTMENT_ID = "ARG_APPOINTMENT_ID"
