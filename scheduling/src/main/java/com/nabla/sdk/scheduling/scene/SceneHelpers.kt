package com.nabla.sdk.scheduling.scene

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.ui.helpers.getSerializableCompat
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import java.util.UUID

internal fun Fragment.setCategoryId(categoryId: CategoryId) {
    val bundle = arguments ?: Bundle()
    bundle.putString(ARG_CATEGORY_ID, categoryId.value.toString())
    arguments = bundle
}

internal fun Fragment.requireCategoryId(): CategoryId {
    return arguments?.getString(ARG_CATEGORY_ID)?.let { CategoryId(UUID.fromString(it)) }
        ?: throwNablaInternalException("Missing category id")
}

private const val ARG_CATEGORY_ID = "ARG_CATEGORY_ID"

internal fun Fragment.setLocation(location: AppointmentLocation) {
    val bundle = arguments ?: Bundle()
    bundle.putSerializable(ARG_LOCATION, location)
    arguments = bundle
}

internal fun Fragment.requireLocation(): AppointmentLocation {
    return arguments?.getSerializableCompat(ARG_LOCATION, AppointmentLocation::class.java)
        ?: throwNablaInternalException("Missing appointment location")
}

private const val ARG_LOCATION = "ARG_LOCATION"
