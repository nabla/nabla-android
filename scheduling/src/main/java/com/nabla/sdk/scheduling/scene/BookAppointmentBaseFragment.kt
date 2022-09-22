package com.nabla.sdk.scheduling.scene

import androidx.annotation.LayoutRes
import com.nabla.sdk.core.domain.entity.InternalException.Companion.asNablaInternal

internal open class BookAppointmentBaseFragment(
    @LayoutRes contentLayoutId: Int
) : SchedulingBaseFragment(contentLayoutId) {

    internal fun hostActivity() = activity as? ScheduleAppointmentActivity
        ?: throw IllegalStateException("Host activity $activity is not a BookAppointmentActivity").asNablaInternal()
}
