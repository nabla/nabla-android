package com.nabla.sdk.scheduling.scene

import androidx.annotation.LayoutRes
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException

internal open class BookAppointmentBaseFragment(
    @LayoutRes contentLayoutId: Int
) : SchedulingBaseFragment(contentLayoutId) {

    internal fun hostActivity() = activity as? ScheduleAppointmentActivity
        ?: throwNablaInternalException("Host activity $activity is not a BookAppointmentActivity")
}
