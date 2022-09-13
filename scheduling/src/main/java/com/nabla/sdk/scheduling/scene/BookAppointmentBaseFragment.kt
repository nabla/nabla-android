package com.nabla.sdk.scheduling.scene

import androidx.annotation.LayoutRes
import com.nabla.sdk.core.domain.entity.InternalException

internal open class BookAppointmentBaseFragment(
    @LayoutRes contentLayoutId: Int
) : SchedulingBaseFragment(contentLayoutId) {

    internal fun hostActivity() = activity as? BookAppointmentActivity
        ?: throw InternalException(IllegalStateException("Host activity $activity is not a BookAppointmentActivity"))
}
